package com.fueledbycaffeine.autoservice.ir

import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import java.io.Closeable
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Registry for tracking service implementations and generating META-INF/services files.
 * 
 * Supports incremental compilation by:
 * - Merging with existing service files
 * - Removing entries for classes that were compiled but no longer have @AutoService
 *
 * Use the [use] method to ensure service files are generated after all IR transformations complete:
 * ```
 * serviceRegistry.use { visitor ->
 *   moduleFragment.accept(visitor, null)
 * }
 * ```
 * 
 * Thread Safety: Uses regular collections since IrGenerationExtension.generate() is called
 * sequentially per module in a single thread. No concurrent access is possible.
 */
internal class ServiceRegistry(
  private val diagnosticReporter: IrDiagnosticReporter,
  private val debugLogger: AutoServiceDebugLogger,
  private val outputDir: Path,
) : Closeable {
  private val providers: MutableMap<String, MutableSet<String>> = mutableMapOf()
  private val compiledClasses: MutableSet<String> = mutableSetOf()
  
  // All classes found in the module - used to validate service file entries
  private val allModuleClasses: MutableSet<String> = mutableSetOf()
  
  /**
   * Track a class that exists in the module's IR.
   * Called for every class visited, not just those with @AutoService.
   */
  fun trackModuleClass(className: String) {
    allModuleClasses.add(className)
  }

  fun register(serviceInterface: String, implementationClass: String) {
    providers.getOrPut(serviceInterface) { sortedSetOf() }.add(implementationClass)
  }

  fun trackCompiledClass(className: String) {
    compiledClasses.add(className)
  }

  /**
   * Called when the use {} block completes. Generates all service files.
   * This is the post-transformation hook that ensures files are written after all IR processing.
   */
  override fun close() {
    generateServiceFiles()
  }

  private fun generateServiceFiles() {
    // Don't return early even if providers is empty - we might need to clean up service files
    // for classes that were compiled but no longer have @AutoService
    
    // Gradle plugin provides the classes directory directly
    val servicesDir = outputDir.resolve("META-INF/services")
    servicesDir.createDirectories()
    debugLogger.log("Creating service files in $servicesDir")

    // Even if no providers were compiled this round, we still need to validate
    // existing service files to handle file deletions in incremental builds.
    // Post-compilation cleanup: validate ALL service files at the end
    // This acts as a cleanup hook to remove stale entries even if we didn't compile those classes
    val existingServiceFiles = when (servicesDir.exists()) {
      true -> servicesDir.listDirectoryEntries().filter { it.isRegularFile() }.toSet()
      else -> emptySet()
    }
    val newServiceFiles = providers.keys.map { servicesDir.resolve(it) }
    
    for (serviceFile in existingServiceFiles + newServiceFiles) {
      val serviceInterface = serviceFile.name
      val implementations = mergeServiceImplementations(serviceFile, serviceInterface)
      writeServiceFile(serviceFile, serviceInterface, implementations)
    }
  }

  private fun mergeServiceImplementations(serviceFile: Path, serviceInterface: String): Set<String> {
    val allImplementations = sortedSetOf<String>()
    
    // Add new implementations from this compilation
    providers[serviceInterface]?.let { allImplementations.addAll(it) }
    
    // Merge with existing entries if file exists
    if (serviceFile.exists()) {
      try {
        val existingEntries = readServiceFile(serviceFile)
        val entriesToKeep = validateExistingEntries(existingEntries, serviceInterface)
        allImplementations.addAll(entriesToKeep)
        
        debugLogger.log("Existing entries: $existingEntries, Compiled classes: $compiledClasses, Keeping: $entriesToKeep")
      } catch (e: Exception) {
        diagnosticReporter.report(
          AutoServiceIrDiagnostics.WARNING,
          "Failed to read existing service file $serviceFile: ${e.message}"
        )
      }
    }
    
    return allImplementations
  }

  private fun validateExistingEntries(existingEntries: List<String>, serviceInterface: String): List<String> {
    // Validate existing entries with aggressive cleanup:
    // - If we compiled the class this round: trust only our current providers map
    // - If we didn't compile it: validate it still exists (compiler + filesystem)
    return existingEntries.filter { entry ->
      if (compiledClasses.contains(entry)) {
        // We compiled this class - only keep if it's still in providers
        providers[serviceInterface]?.contains(entry) == true
      } else {
        // We didn't compile this class - validate it with both compiler and filesystem
        // This catches deletions in incremental builds
        doesClassExist(entry)
      }
    }
  }

  private fun writeServiceFile(serviceFile: Path, serviceInterface: String, implementations: Set<String>) {
    debugLogger.log("Writing service file: $serviceFile")
    
    try {
      if (implementations.isEmpty()) {
        deleteServiceFileIfExists(serviceFile)
        debugLogger.log("Deleted empty service file: $serviceFile")
      } else {
        serviceFile.writeText(implementations.sorted().joinToString("\n") + "\n")
        debugLogger.log(
          "Service file contents for $serviceInterface:\n" +
            implementations.sorted().joinToString("\n")
        )
      }
    } catch (e: Exception) {
      diagnosticReporter.report(
        AutoServiceIrDiagnostics.ERROR,
        "Failed to write service file $serviceFile: ${e.message}"
      )
    }
  }

  private fun deleteServiceFileIfExists(serviceFile: Path) {
    if (serviceFile.exists()) {
      serviceFile.deleteExisting()
      debugLogger.log("Deleted empty service file: $serviceFile")
    }
  }

  private fun readServiceFile(serviceFile: Path): List<String> {
    return serviceFile.readLines()
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.startsWith("#") }
  }

  private fun doesClassExist(fqcn: String): Boolean {
    // Phase 1: If the class is in the current module's IR, it definitely exists
    if (fqcn in allModuleClasses) {
      return true
    }
    
    // Phase 2: Check if the class file exists
    // In incremental builds, unchanged files won't be in the module but their class files exist
    // When a source file is deleted, IC deletes the class file, so this returns false
    val classFilePath = fqcn.replace('.', '/') + ".class"
    return outputDir.resolve(classFilePath).exists()
  }
}

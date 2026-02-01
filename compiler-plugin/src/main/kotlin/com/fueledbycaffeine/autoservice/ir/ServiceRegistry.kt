package com.fueledbycaffeine.autoservice.ir

import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import java.io.Closeable
import java.io.File

/**
 * Registry for tracking service implementations and generating META-INF/services files.
 * 
 * Supports incremental compilation by:
 * - Merging with existing service files
 * - Removing entries for classes that were compiled but no longer have @AutoService
 */
internal class ServiceRegistry(
  private val diagnosticReporter: IrDiagnosticReporter,
  private val debug: Boolean,
  private val outputDirPath: String?,
) : Closeable {
  private val providers = mutableMapOf<String, MutableSet<String>>()
  private val compiledClasses = mutableSetOf<String>()
  
  // All classes found in the module - used to validate service file entries
  private val allModuleClasses = mutableSetOf<String>()
  
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
    
    val outputDir = if (outputDirPath != null) {
      File(outputDirPath)
    } else {
      getOutputDirectory()
    }
    
    if (debug) {
      diagnosticReporter.report(
        AutoServiceIrDiagnostics.INFO,
        "AutoService: Output dir path = $outputDirPath, resolved dir = ${outputDir?.absolutePath}"
      )
    }
    
    if (outputDir == null) {
      diagnosticReporter.report(
        AutoServiceIrDiagnostics.WARNING,
        "Could not determine output directory for service files"
      )
      return
    }

    // Determine the correct location for META-INF/services
    // If outputDirPath is provided (from Gradle plugin), use it directly
    // Otherwise, check if we need to add a 'classes' subdirectory (kotlin-compile-testing case)
    val servicesDir = if (outputDirPath != null) {
      // Gradle plugin provides the classes directory directly
      File(outputDir, "META-INF/services")
    } else {
      // kotlin-compile-testing or other uses - add classes subdir if needed
      val classesDir = File(outputDir, "classes")
      if (classesDir.exists() || outputDir.name != "classes") {
        File(classesDir, "META-INF/services")
      } else {
        File(outputDir, "META-INF/services")
      }
    }
    servicesDir.mkdirs()
    
    diagnosticReporter.report(
      AutoServiceIrDiagnostics.INFO,
      "AutoService: Creating service files in ${servicesDir.absolutePath}"
    )

    // Even if no providers were compiled this round, we still need to validate
    // existing service files to handle file deletions in incremental builds.
    // Post-compilation cleanup: validate ALL service files at the end
    // This acts as a cleanup hook to remove stale entries even if we didn't compile those classes
    val allServiceFiles = if (servicesDir.exists()) {
      servicesDir.listFiles()?.filter { it.isFile } ?: emptyList()
    } else {
      emptyList()
    }
    val allServiceInterfaces = (providers.keys + allServiceFiles.map { it.name }).toSet()
    
    for (serviceInterface in allServiceInterfaces) {
      val serviceFile = File(servicesDir, serviceInterface)
      
      // For incremental compilation: merge with existing service file entries
      val allImplementations = sortedSetOf<String>()
      
      // Add new implementations from this compilation
      providers[serviceInterface]?.let { allImplementations.addAll(it) }
      
      if (serviceFile.exists()) {
        try {
          val existingEntries = serviceFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
          
          // Validate existing entries with aggressive cleanup:
          // - If we compiled the class this round: trust only our current providers map
          // - If we didn't compile it: validate it still exists (compiler + filesystem)
          val entriesToKeep = existingEntries.filter { entry ->
            if (compiledClasses.contains(entry)) {
              // We compiled this class - only keep if it's still in providers
              providers[serviceInterface]?.contains(entry) == true
            } else {
              // We didn't compile this class - validate it with both compiler and filesystem
              // This catches deletions in incremental builds
              doesClassExist(outputDir, entry)
            }
          }
          
          allImplementations.addAll(entriesToKeep)
          
          if (debug) {
            diagnosticReporter.report(
              AutoServiceIrDiagnostics.INFO,
              "Existing entries: $existingEntries, Compiled classes: $compiledClasses, Keeping: $entriesToKeep"
            )
          }
        } catch (e: Exception) {
          diagnosticReporter.report(
            AutoServiceIrDiagnostics.WARNING,
            "Failed to read existing service file ${serviceFile.absolutePath}: ${e.message}"
          )
        }
      }
      
      if (debug) {
        diagnosticReporter.report(
          AutoServiceIrDiagnostics.INFO,
          "Writing service file: ${serviceFile.absolutePath}"
        )
      }

      try {
        if (allImplementations.isEmpty()) {
          // Remove service file if no implementations remain
          if (serviceFile.exists()) {
            serviceFile.delete()
            if (debug) {
              diagnosticReporter.report(
                AutoServiceIrDiagnostics.INFO,
                "Deleted empty service file: ${serviceFile.absolutePath}"
              )
            }
          }
        } else {
          serviceFile.writeText(allImplementations.sorted().joinToString("\n") + "\n")
          
          if (debug) {
            diagnosticReporter.report(
              AutoServiceIrDiagnostics.INFO,
              "Service file contents for $serviceInterface: ${allImplementations.sorted()}"
            )
          }
        }
      } catch (e: Exception) {
        diagnosticReporter.report(
          AutoServiceIrDiagnostics.ERROR,
          "Failed to write service file ${serviceFile.absolutePath}: ${e.message}"
        )
      }
    }
  }

  private fun getOutputDirectory(): File? {
    // Try to get the output directory from system properties or environment
    // This is where compiled classes go, and we want META-INF/services next to them
    val outputPath = System.getProperty("kotlin.output.dir")
      ?: System.getProperty("kotlin.compiler.output.path")
      ?: System.getProperty("org.jetbrains.kotlin.compiler.output.path")
    
    if (outputPath != null) {
      return File(outputPath)
    }
    
    // Fallback: use user.dir/build/classes/kotlin/main
    val userDir = System.getProperty("user.dir")
    return if (userDir != null) {
      File(userDir, "build/classes/kotlin/main")
    } else {
      null
    }
  }

  private fun doesClassExist(
    outputDir: File?,
    fullyQualifiedClassName: String,
  ): Boolean {
    // Phase 1: If the class is in the current module's IR, it definitely exists
    if (fullyQualifiedClassName in allModuleClasses) {
      return true
    }
    
    // Phase 2: Check if the class file exists
    // In incremental builds, unchanged files won't be in the module but their class files exist
    // When a source file is deleted, IC deletes the class file, so this returns false
    return outputDir?.let { dir ->
      val classFilePath = fullyQualifiedClassName.replace('.', '/') + ".class"
      File(dir, classFilePath).exists()
    } ?: true
  }

}

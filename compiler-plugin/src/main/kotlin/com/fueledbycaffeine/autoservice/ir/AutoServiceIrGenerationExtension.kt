package com.fueledbycaffeine.autoservice.ir

import com.fueledbycaffeine.autoservice.AutoServiceSymbols
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import java.nio.file.Path

internal class AutoServiceIrGenerationExtension(
  private val outputDir: Path,
  private val debugLogDir: Path?,
) : IrGenerationExtension {

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val diagnosticReporter = pluginContext.diagnosticReporter
    val debugLogger = AutoServiceDebugLogger(debugLogDir)
    
    // Use the use {} pattern to ensure service files are generated after all IR transformations.
    // This acts as a post-transformation hook - when the block completes, close() is called
    // which writes all collected service registrations to META-INF/services files.
    ServiceRegistry(diagnosticReporter, debugLogger, outputDir).use { serviceRegistry ->
      val autoServiceVisitor =
        AutoServiceIrVisitor(
          diagnosticReporter,
          serviceRegistry,
          debugLogger,
        )
      moduleFragment.accept(autoServiceVisitor, null)
    }
    
    // Strip synthetic __AutoService__ mirror classes - they were only needed for FIR
    // incremental compilation tracking and serve no purpose in the final bytecode.
    stripMirrorClasses(moduleFragment, debugLogger)
  }
  
  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private fun stripMirrorClasses(moduleFragment: IrModuleFragment, debugLogger: AutoServiceDebugLogger) {
    val topLevelClasses = moduleFragment.files.flatMap { file ->
      file.declarations.filterIsInstance<IrClass>()
    }
    stripMirrorClassesFromClasses(topLevelClasses, debugLogger)
  }

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private tailrec fun stripMirrorClassesFromClasses(
    pending: List<IrClass>,
    debugLogger: AutoServiceDebugLogger,
  ) {
    if (pending.isEmpty()) return

    val irClass = pending.first()
    val rest = pending.drop(1)

    // Remove __AutoService__ nested classes
    val mirrorsToRemove = irClass.declarations.filterIsInstance<IrClass>()
      .filter { it.name == AutoServiceSymbols.Names.MIRROR_CLASS }

    for (mirror in mirrorsToRemove) {
      debugLogger.log("Stripping mirror class: ${irClass.name}\$${mirror.name}")
      irClass.declarations.remove(mirror)
    }

    // Add remaining nested classes to the pending list
    val nestedClasses = irClass.declarations.filterIsInstance<IrClass>()

    stripMirrorClassesFromClasses(rest + nestedClasses, debugLogger)
  }
}

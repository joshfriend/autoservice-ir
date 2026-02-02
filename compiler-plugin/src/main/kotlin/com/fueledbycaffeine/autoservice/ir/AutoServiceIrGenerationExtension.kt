package com.fueledbycaffeine.autoservice.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
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
  }
}

package com.fueledbycaffeine.autoservice.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

internal class AutoServiceIrGenerationExtension(
  private val debug: Boolean,
  private val outputDir: String?,
  private val projectRoot: String?,
) : IrGenerationExtension {

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val diagnosticReporter = pluginContext.diagnosticReporter
    val debugLogger = if (debug) AutoServiceDebugLogger(outputDir) else null
    
    // Use the use {} pattern to ensure service files are generated after all IR transformations.
    // This acts as a post-transformation hook - when the block completes, close() is called
    // which writes all collected service registrations to META-INF/services files.
    ServiceRegistry(diagnosticReporter, debugLogger, outputDir).use { serviceRegistry ->
      val autoServiceVisitor =
        AutoServiceIrVisitor(
          pluginContext,
          diagnosticReporter,
          serviceRegistry,
          debugLogger,
          projectRoot,
        )
      moduleFragment.accept(autoServiceVisitor, null)
    }
  }
}

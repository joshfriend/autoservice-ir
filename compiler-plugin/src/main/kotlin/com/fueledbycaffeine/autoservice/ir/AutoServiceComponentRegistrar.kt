package com.fueledbycaffeine.autoservice.ir

import com.fueledbycaffeine.autoservice.fir.AutoServiceFirExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import java.nio.file.Path

/**
 * Registers the AutoService compiler extensions.
 * 
 * This includes:
 * - FIR extension: Generates synthetic mirror declarations for IC support
 * - IR extension: Processes @AutoService annotations and generates service files
 * 
 * This class is registered via META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
 */
public class AutoServiceComponentRegistrar : CompilerPluginRegistrar() {

  override val pluginId: String = AutoServiceCommandLineProcessor.PLUGIN_ID

  override val supportsK2: Boolean = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    // Try to get output directory from configuration (set by Gradle plugin)
    val outputDir = configuration[KEY_OUTPUT_DIR]?.let {
      Path.of(it)
    } ?: run {
      // Fallback for testing scenarios
      configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)?.toPath()
        ?: error("Output directory not specified")
    }
    val debugLogDir = configuration[KEY_DEBUG_LOG_DIR]?.let { Path.of(it) }

    // Register FIR extension for IC support via synthetic mirror declarations
    FirExtensionRegistrarAdapter.registerExtension(AutoServiceFirExtensionRegistrar())

    // Register IR extension for service file generation
    IrGenerationExtension.registerExtension(
      AutoServiceIrGenerationExtension(outputDir, debugLogDir)
    )
  }
}

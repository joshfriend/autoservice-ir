package com.fueledbycaffeine.autoservice.ir

import com.fueledbycaffeine.autoservice.fir.AutoServiceFirExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

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

  override val pluginId: String = "com.fueledbycaffeine.autoservice.compiler"

  override val supportsK2: Boolean = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val debug = configuration[KEY_DEBUG] == true
    val outputDir = configuration[KEY_OUTPUT_DIR]
    val projectRoot = configuration[KEY_PROJECT_ROOT]

    // Register FIR extension for IC support via synthetic mirror declarations
    FirExtensionRegistrarAdapter.registerExtension(AutoServiceFirExtensionRegistrar())

    // Register IR extension for service file generation
    IrGenerationExtension.registerExtension(
      AutoServiceIrGenerationExtension(debug, outputDir, projectRoot)
    )
  }
}

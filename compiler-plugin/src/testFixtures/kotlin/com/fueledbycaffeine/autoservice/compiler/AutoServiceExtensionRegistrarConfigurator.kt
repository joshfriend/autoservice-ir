// Copyright (C) 2024 FueledByCaffeine
// SPDX-License-Identifier: Apache-2.0
package com.fueledbycaffeine.autoservice.compiler

import com.fueledbycaffeine.autoservice.fir.AutoServiceFirExtensionRegistrar
import com.fueledbycaffeine.autoservice.ir.AutoServiceIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Extension function to configure the AutoService plugin for tests.
 */
fun TestConfigurationBuilder.configurePlugin() {
  useConfigurators(
    ::AutoServiceExtensionRegistrarConfigurator,
    ::AutoServiceRuntimeEnvironmentConfigurator,
  )

  useDirectives(AutoServiceDirectives)

  useCustomRuntimeClasspathProviders(::AutoServiceRuntimeClassPathProvider)

  useSourcePreprocessor(::AutoServiceDefaultImportPreprocessor)
}

/**
 * Configures the AutoService compiler extensions for test compilation.
 */
class AutoServiceExtensionRegistrarConfigurator(testServices: TestServices) :
  EnvironmentConfigurator(testServices) {
  override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
    module: TestModule,
    configuration: CompilerConfiguration,
  ) {
    // Register FIR extension for checkers and IC support (always needed)
    FirExtensionRegistrarAdapter.registerExtension(AutoServiceFirExtensionRegistrar())

    // Try to get output directory from configuration - only needed for IR tests
    val outputDir = configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)?.toPath()
    
    // Only register IR extension if output directory is available (box tests)
    // Diagnostic tests don't need IR processing
    if (outputDir != null) {
      IrGenerationExtension.registerExtension(
        AutoServiceIrGenerationExtension(outputDir, debugLogDir = null)
      )
    }
  }
}

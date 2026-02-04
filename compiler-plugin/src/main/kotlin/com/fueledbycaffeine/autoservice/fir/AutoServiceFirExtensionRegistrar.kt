package com.fueledbycaffeine.autoservice.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * Registers FIR extensions for AutoService.
 * 
 * Key extensions:
 * - [AutoServiceFirCheckersExtension]: Provides IDE error checking and stores metadata for IR phase
 */
internal class AutoServiceFirExtensionRegistrar : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +::AutoServiceFirCheckersExtension

    registerDiagnosticContainers(AutoServiceDiagnostics)
  }
}

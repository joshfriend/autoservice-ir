package com.fueledbycaffeine.autoservice.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * Registers FIR extensions for AutoService.
 * 
 * Key extensions:
 * - [AutoServiceMirrorFirGenerator]: Generates synthetic "mirror" declarations for incremental
 *   compilation support
 * - [AutoServiceFirCheckersExtension]: Provides IDE error checking without requiring a full build
 */
internal class AutoServiceFirExtensionRegistrar : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +::AutoServiceMirrorFirGenerator
    +::AutoServiceFirCheckersExtension

    registerDiagnosticContainers(AutoServiceDiagnostics)
  }
}

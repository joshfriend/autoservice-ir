package com.fueledbycaffeine.autoservice.fir

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.NAME_IDENTIFIER
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers.STRING
import org.jetbrains.kotlin.psi.KtElement

/**
 * Diagnostic errors for FIR-based AutoService validation.
 *
 * These diagnostics are reported during FIR analysis, enabling IDE error highlighting
 * without requiring a full build.
 */
internal object AutoServiceFirErrors : KtDiagnosticsContainer() {
  val AUTOSERVICE_WRONG_CLASS_KIND by error0<KtElement>(NAME_IDENTIFIER)
  val AUTOSERVICE_NON_PUBLIC_CLASS by error0<KtElement>(NAME_IDENTIFIER)
  val AUTOSERVICE_MISSING_SERVICE_INTERFACE by error0<KtElement>(NAME_IDENTIFIER)
  val AUTOSERVICE_DOES_NOT_IMPLEMENT_SERVICE by error2<KtElement, String, String>(NAME_IDENTIFIER)

  override fun getRendererFactory(): BaseDiagnosticRendererFactory {
    return AutoServiceFirErrorsRendererFactory
  }
}

/**
 * Renderer factory for AutoService FIR diagnostic messages.
 */
private object AutoServiceFirErrorsRendererFactory : BaseDiagnosticRendererFactory() {
  override val MAP by KtDiagnosticFactoryToRendererMap("AutoService") { map ->
    map.apply {
      put(
        AutoServiceFirErrors.AUTOSERVICE_WRONG_CLASS_KIND,
        "@AutoService must be applied to a non-abstract class"
      )
      put(
        AutoServiceFirErrors.AUTOSERVICE_NON_PUBLIC_CLASS,
        "@AutoService classes must be public or internal"
      )
      put(
        AutoServiceFirErrors.AUTOSERVICE_MISSING_SERVICE_INTERFACE,
        "@AutoService requires a service interface: either specify it explicitly or ensure the class has exactly one supertype"
      )
      put(
        AutoServiceFirErrors.AUTOSERVICE_DOES_NOT_IMPLEMENT_SERVICE,
        "ServiceProviders must implement their service provider interface. ''{0}'' does not implement ''{1}''",
        STRING,
        STRING
      )
    }
  }
}

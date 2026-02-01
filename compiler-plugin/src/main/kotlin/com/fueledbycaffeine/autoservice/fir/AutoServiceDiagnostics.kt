package com.fueledbycaffeine.autoservice.fir

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.MODALITY_MODIFIER
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.NAME_IDENTIFIER
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.VISIBILITY_MODIFIER
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers.STRING
import org.jetbrains.kotlin.psi.KtElement

/**
 * Diagnostic errors for FIR-based AutoService validation.
 *
 * These diagnostics are reported during FIR analysis, enabling IDE error highlighting
 * without requiring a full build.
 */
internal object AutoServiceDiagnostics : KtDiagnosticsContainer() {
  // Generic error/warning with custom message
  val AUTOSERVICE_ERROR by error1<KtElement, String>(NAME_IDENTIFIER)
  val AUTOSERVICE_WARNING by warning1<KtElement, String>(NAME_IDENTIFIER)
  
  // Specific errors with positioning strategies
  val AUTOSERVICE_ABSTRACT_CLASS by error0<KtElement>(MODALITY_MODIFIER)
  val AUTOSERVICE_WRONG_CLASS_KIND by error1<KtElement, String>(NAME_IDENTIFIER)
  val AUTOSERVICE_VISIBILITY_ERROR by error0<KtElement>(VISIBILITY_MODIFIER)
  val AUTOSERVICE_MISSING_SERVICE_INTERFACE by error0<KtElement>(NAME_IDENTIFIER)
  val AUTOSERVICE_DOES_NOT_IMPLEMENT by error1<KtElement, String>(NAME_IDENTIFIER)

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
      // Generic messages
      put(AutoServiceDiagnostics.AUTOSERVICE_ERROR, "{0}", STRING)
      put(AutoServiceDiagnostics.AUTOSERVICE_WARNING, "{0}", STRING)
      
      // Specific messages
      put(
        AutoServiceDiagnostics.AUTOSERVICE_ABSTRACT_CLASS,
        "@AutoService cannot be applied to an abstract class"
      )
      put(
        AutoServiceDiagnostics.AUTOSERVICE_WRONG_CLASS_KIND,
        "@AutoService cannot be applied to {0}",
        STRING
      )
      put(
        AutoServiceDiagnostics.AUTOSERVICE_VISIBILITY_ERROR,
        "@AutoService classes must be public or internal"
      )
      put(
        AutoServiceDiagnostics.AUTOSERVICE_MISSING_SERVICE_INTERFACE,
        "@AutoService requires a service interface: either specify it explicitly or ensure the class has exactly one supertype"
      )
      put(
        AutoServiceDiagnostics.AUTOSERVICE_DOES_NOT_IMPLEMENT,
        "@AutoService class does not implement {0}",
        STRING
      )
    }
  }
}


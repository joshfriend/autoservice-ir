package com.fueledbycaffeine.autoservice.ir

import org.jetbrains.kotlin.backend.common.KtDefaultCommonBackendErrorMessages
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity

internal object AutoServiceIrDiagnostics {
  private val rendererFactory = KtDefaultCommonBackendErrorMessages
  private val rendererMap = rendererFactory.MAP

  val INFO = KtSourcelessDiagnosticFactory("AUTOSERVICE_IR_INFO", Severity.INFO, rendererFactory)
  val WARNING = KtSourcelessDiagnosticFactory("AUTOSERVICE_IR_WARNING", Severity.WARNING, rendererFactory)
  val ERROR = KtSourcelessDiagnosticFactory("AUTOSERVICE_IR_ERROR", Severity.ERROR, rendererFactory)

  init {
    rendererMap.put(INFO, "{0}")
    rendererMap.put(WARNING, "{0}")
    rendererMap.put(ERROR, "{0}")
  }
}
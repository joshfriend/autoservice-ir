package com.fueledbycaffeine.autoservice.ir

import org.jetbrains.kotlin.backend.common.KtDefaultCommonBackendErrorMessages
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity
import java.io.File

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

/**
 * Debug logger that writes to a file in the output directory.
 */
internal class AutoServiceDebugLogger(outputDir: String?) {
  private val logFile: File? = outputDir?.let { 
    File(it, "autoservice-debug.log").also { file ->
      file.parentFile?.mkdirs()
      file.writeText("AutoService Debug Log\n" + "=".repeat(50) + "\n")
    }
  }
  
  fun log(message: String) {
    logFile?.appendText("$message\n")
  }
}
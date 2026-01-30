package com.fueledbycaffeine.autoservice.ir

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal val KEY_DEBUG =
  CompilerConfigurationKey<Boolean>("Enable/disable debug logging on the given compilation")
internal val KEY_OUTPUT_DIR =
  CompilerConfigurationKey<String>("Output directory for generated service files")
internal val KEY_PROJECT_ROOT =
  CompilerConfigurationKey<String>("Project root directory for relative path display")

/**
 * Processes command line options for the AutoService compiler plugin.
 * 
 * This class is registered via META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
 */
public class AutoServiceCommandLineProcessor : CommandLineProcessor {
  internal companion object {
    val OPTION_DEBUG =
      CliOption(
        optionName = "debug",
        valueDescription = "<true | false>",
        description = KEY_DEBUG.toString(),
        required = false,
        allowMultipleOccurrences = false,
      )
    val OPTION_OUTPUT_DIR =
      CliOption(
        optionName = "outputDir",
        valueDescription = "String",
        description = KEY_OUTPUT_DIR.toString(),
        required = false,
        allowMultipleOccurrences = false,
      )
    val OPTION_PROJECT_ROOT =
      CliOption(
        optionName = "projectRoot",
        valueDescription = "String",
        description = KEY_PROJECT_ROOT.toString(),
        required = false,
        allowMultipleOccurrences = false,
      )
  }

  override val pluginId: String = "com.fueledbycaffeine.autoservice.compiler"

  override val pluginOptions: Collection<AbstractCliOption> =
    listOf(OPTION_DEBUG, OPTION_OUTPUT_DIR, OPTION_PROJECT_ROOT)

  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration,
  ): Unit =
    when (option.optionName) {
      "debug" -> configuration.put(KEY_DEBUG, value.toBoolean())
      "outputDir" -> configuration.put(KEY_OUTPUT_DIR, value)
      "projectRoot" -> configuration.put(KEY_PROJECT_ROOT, value)
      else -> error("Unknown plugin option: ${option.optionName}")
    }
}

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
  public companion object {
    /** Compiler plugin ID for the AutoService plugin. */
    public const val PLUGIN_ID: String = "com.fueledbycaffeine.autoservice.compiler"
    
    /** Option name for debug logging. */
    public const val OPTION_NAME_DEBUG: String = "debug"
    
    /** Option name for output directory. */
    public const val OPTION_NAME_OUTPUT_DIR: String = "outputDir"
    
    /** Option name for project root directory. */
    public const val OPTION_NAME_PROJECT_ROOT: String = "projectRoot"
    
    internal val OPTION_DEBUG =
      CliOption(
        optionName = OPTION_NAME_DEBUG,
        valueDescription = "<true | false>",
        description = KEY_DEBUG.toString(),
        required = false,
        allowMultipleOccurrences = false,
      )
    internal val OPTION_OUTPUT_DIR =
      CliOption(
        optionName = OPTION_NAME_OUTPUT_DIR,
        valueDescription = "String",
        description = KEY_OUTPUT_DIR.toString(),
        required = false,
        allowMultipleOccurrences = false,
      )
    internal val OPTION_PROJECT_ROOT =
      CliOption(
        optionName = OPTION_NAME_PROJECT_ROOT,
        valueDescription = "String",
        description = KEY_PROJECT_ROOT.toString(),
        required = false,
        allowMultipleOccurrences = false,
      )
  }

  override val pluginId: String = PLUGIN_ID

  override val pluginOptions: Collection<AbstractCliOption> =
    listOf(OPTION_DEBUG, OPTION_OUTPUT_DIR, OPTION_PROJECT_ROOT)

  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration,
  ): Unit =
    when (option.optionName) {
      OPTION_NAME_DEBUG -> configuration.put(KEY_DEBUG, value.toBoolean())
      OPTION_NAME_OUTPUT_DIR -> configuration.put(KEY_OUTPUT_DIR, value)
      OPTION_NAME_PROJECT_ROOT -> configuration.put(KEY_PROJECT_ROOT, value)
      else -> error("Unknown plugin option: ${option.optionName}")
    }
}

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
 * and is used by the Kotlin compiler to parse plugin-specific command line arguments.
 * 
 * ## Usage
 * 
 * These constants are primarily used by:
 * - The [AutoServiceGradlePlugin][com.fueledbycaffeine.autoservice.gradle.AutoServiceGradlePlugin] 
 *   to pass options to the compiler
 * - Custom build tool integrations that need to invoke the compiler plugin directly
 * 
 * @see AutoServiceComponentRegistrar for the plugin registration
 */
public class AutoServiceCommandLineProcessor : CommandLineProcessor {
  public companion object {
    /**
     * The unique identifier for the AutoService compiler plugin.
     * 
     * This ID is used by the Kotlin compiler to identify and route options to this plugin.
     * It must match the [AutoServiceComponentRegistrar.pluginId].
     */
    public const val PLUGIN_ID: String = "com.fueledbycaffeine.autoservice.compiler"
    
    /**
     * Option name for enabling debug logging.
     * 
     * When enabled, the plugin outputs detailed information about:
     * - Which classes are being processed
     * - Inferred service interfaces
     * - Service file generation
     * 
     * Value: `"true"` or `"false"` (default: `"false"`)
     */
    public const val OPTION_NAME_DEBUG: String = "debug"
    
    /**
     * Option name for specifying the output directory for generated service files.
     * 
     * This should be the classes output directory where `META-INF/services` will be created.
     * The Gradle plugin automatically sets this to the compilation's classes directory.
     * 
     * Value: Absolute path to the output directory
     */
    public const val OPTION_NAME_OUTPUT_DIR: String = "outputDir"
    
    /**
     * Option name for specifying the project root directory.
     * 
     * Used for displaying relative paths in error messages for better IDE integration.
     * 
     * Value: Absolute path to the project root
     */
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

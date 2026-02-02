package com.fueledbycaffeine.autoservice.ir

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal val KEY_OUTPUT_DIR =
  CompilerConfigurationKey<String>("Output directory for generated service files")
internal val KEY_DEBUG_LOG_DIR =
  CompilerConfigurationKey<String>("Output directory for debug log files (enables debugging if set)")

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
     * Option name for specifying the output directory for generated service files.
     * 
     * This should be the classes output directory where `META-INF/services` will be created.
     * The Gradle plugin automatically sets this to the compilation's classes directory.
     * 
     * Value: Absolute path to the output directory
     */
    public const val OPTION_NAME_OUTPUT_DIR: String = "outputDir"
    
    /**
     * Option name for specifying the output directory for debug log files.
     * 
     * When set, enables debug logging for the plugin.
     * Debug logs include detailed information about:
     * - Which classes are being processed
     * - Inferred service interfaces
     * - Service file generation
     * 
     * This should be a dedicated directory in the project's build folder.
     * The Gradle plugin automatically sets this to a subdirectory of the build directory.
     * 
     * Value: Absolute path to the debug log directory (or omit to disable debugging)
     */
    public const val OPTION_NAME_DEBUG_LOG_DIR: String = "debugLogDir"

    internal val OPTION_OUTPUT_DIR =
      CliOption(
        optionName = OPTION_NAME_OUTPUT_DIR,
        valueDescription = "String",
        description = KEY_OUTPUT_DIR.toString(),
        required = false,
        allowMultipleOccurrences = false,
      )
    internal val OPTION_DEBUG_LOG_DIR =
      CliOption(
        optionName = OPTION_NAME_DEBUG_LOG_DIR,
        valueDescription = "String",
        description = KEY_DEBUG_LOG_DIR.toString(),
        required = false,
        allowMultipleOccurrences = false,
      )
  }

  override val pluginId: String = PLUGIN_ID

  override val pluginOptions: Collection<AbstractCliOption> =
    listOf(OPTION_OUTPUT_DIR, OPTION_DEBUG_LOG_DIR)

  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration,
  ): Unit =
    when (option.optionName) {
      OPTION_NAME_OUTPUT_DIR -> configuration.put(KEY_OUTPUT_DIR, value)
      OPTION_NAME_DEBUG_LOG_DIR -> configuration.put(KEY_DEBUG_LOG_DIR, value)
      else -> error("Unknown plugin option: ${option.optionName}")
    }
}

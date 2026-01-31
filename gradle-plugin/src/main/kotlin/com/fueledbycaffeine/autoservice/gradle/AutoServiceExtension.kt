package com.fueledbycaffeine.autoservice.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

/**
 * Configuration extension for the AutoService compiler plugin.
 * 
 * Options can be configured via the DSL:
 * ```kotlin
 * autoService {
 *     debug(true)
 * }
 * ```
 * 
 * Or via Gradle properties (in gradle.properties or via -P flag):
 * ```properties
 * autoservice.debug=true
 * ```
 * 
 * DSL configuration takes precedence over Gradle properties.
 */
public abstract class AutoServiceExtension @Inject constructor(
  objects: ObjectFactory,
  providers: ProviderFactory,
) {
  
  internal val debug: Property<Boolean> = objects.property(Boolean::class.java)
    .convention(providers.gradleProperty(PROPERTY_DEBUG).map { it.toBoolean() }.orElse(false))

  /**
   * Enable debug logging.
   * 
   * Can also be set via Gradle property: `autoservice.debug=true`
   * 
   * Default: false
   */
  public fun debug(value: Boolean) {
    debug.set(value)
    debug.disallowChanges()
  }

  public companion object {
    public const val NAME: String = "autoService"
    public const val PROPERTY_DEBUG: String = "autoservice.debug"

    public fun create(container: ExtensionContainer): AutoServiceExtension {
      return container.create(NAME, AutoServiceExtension::class.java)
    }
  }
}

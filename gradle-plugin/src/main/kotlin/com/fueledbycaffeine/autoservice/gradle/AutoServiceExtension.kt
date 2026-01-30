package com.fueledbycaffeine.autoservice.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class AutoServiceExtension @Inject constructor(objects: ObjectFactory) {
  
  internal val debug: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

  /**
   * Enable debug logging.
   * Default: false
   */
  public fun debug(value: Boolean) {
    debug.set(value)
    debug.disallowChanges()
  }
}

package com.fueledbycaffeine.autoservice

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Centralized symbols and identifiers for the AutoService compiler plugin.
 *
 * This object provides type-safe references to annotation classes and other
 * identifiers used throughout the plugin, avoiding string literals scattered
 * across the codebase.
 */
@Suppress("unused") // Referenced throughout the compiler plugin
internal object AutoServiceSymbols {

  /**
   * ClassIds for annotations processed by the plugin.
   */
  object ClassIds {
    /**
     * Our AutoService annotation: `com.fueledbycaffeine.autoservice.AutoService`
     */
    val AUTOSERVICE: ClassId = ClassId.topLevel(FqName("com.fueledbycaffeine.autoservice.AutoService"))

    /**
     * Google's AutoService annotation: `com.google.auto.service.AutoService`
     *
     * Supported for backwards compatibility with existing codebases.
     */
    val GOOGLE_AUTOSERVICE: ClassId = ClassId.topLevel(FqName("com.google.auto.service.AutoService"))

    /**
     * Kotlin's Suppress annotation for suppressing AutoService warnings.
     */
    val SUPPRESS: ClassId = ClassId.topLevel(FqName("kotlin.Suppress"))
  }

  /**
   * FqNames for use in FIR predicates and lookups.
   */
  object FqNames {
    val AUTOSERVICE: FqName = ClassIds.AUTOSERVICE.asSingleFqName()
    val GOOGLE_AUTOSERVICE: FqName = ClassIds.GOOGLE_AUTOSERVICE.asSingleFqName()
  }

  /**
   * Special identifier names used by the plugin.
   */
  object Names {
    /**
     * Name of the synthetic mirror class generated for incremental compilation support.
     */
    val MIRROR_CLASS: Name = Name.identifier("__AutoService__")
  }
}

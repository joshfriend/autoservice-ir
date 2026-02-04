// Copyright (C) 2024 FueledByCaffeine
// SPDX-License-Identifier: Apache-2.0
package com.fueledbycaffeine.autoservice.compiler

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.FULL_JDK
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JVM_TARGET
import org.jetbrains.kotlin.test.runners.AbstractFirLightTreeDiagnosticsTest
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

/**
 * Base class for diagnostic tests that verify compiler errors and warnings.
 * 
 * Diagnostic tests check that the compiler reports expected errors/warnings
 * for invalid @AutoService usage. Expected diagnostics are specified in
 * companion `.diag.txt` files.
 */
open class AbstractDiagnosticTest : AbstractFirLightTreeDiagnosticsTest() {
  override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
    return ClasspathBasedStandardLibrariesPathProvider
  }

  override fun configure(builder: TestConfigurationBuilder) {
    super.configure(builder)

    with(builder) {
      // Use custom assertions that show actual diff when tests fail
      assertions = DiffShowingAssertions

      configurePlugin()

      defaultDirectives {
        JVM_TARGET.with(
          JvmTarget.fromString(System.getProperty("autoservice.jvmTarget", JvmTarget.JVM_17.description))!!
        )
        +FULL_JDK
      }
    }
  }
}

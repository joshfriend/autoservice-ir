// Copyright (C) 2024 FueledByCaffeine
// SPDX-License-Identifier: Apache-2.0
package com.fueledbycaffeine.autoservice.compiler

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_DEXING
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.FULL_JDK
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JVM_TARGET
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

/**
 * Base class for "box" tests that verify the runtime behavior of generated code.
 * 
 * Box tests run the compiled code and verify it works correctly at runtime.
 * Test files must contain a `box()` function that returns "OK" on success.
 */
open class AbstractBoxTest : AbstractFirLightTreeBlackBoxCodegenTest() {
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
        +WITH_STDLIB

        +IGNORE_DEXING // Avoids loading R8 from the classpath.
      }
    }
  }
}

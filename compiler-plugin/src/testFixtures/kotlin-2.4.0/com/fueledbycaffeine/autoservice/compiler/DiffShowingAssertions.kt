package com.fueledbycaffeine.autoservice.compiler

import org.jetbrains.kotlin.test.services.JUnit5Assertions
import kotlin.time.Duration

/**
 * Kotlin 2.4.0+ variant: implements [assertTimeoutPreemptively], which was added as an
 * abstract member of `AssertionsService` in 2.4.0.
 */
object DiffShowingAssertions : DiffShowingAssertionsBase() {

  override fun assertTimeoutPreemptively(timeout: Duration, message: () -> String, action: () -> Unit) {
    JUnit5Assertions.assertTimeoutPreemptively(timeout, message, action)
  }
}

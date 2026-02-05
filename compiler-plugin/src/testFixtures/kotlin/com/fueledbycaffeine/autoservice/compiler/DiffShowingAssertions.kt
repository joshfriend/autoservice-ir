// Copyright (C) 2024 FueledByCaffeine
// SPDX-License-Identifier: Apache-2.0
package com.fueledbycaffeine.autoservice.compiler

import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.util.convertLineSeparators
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.opentest4j.AssertionFailedError
import java.io.File

/**
 * Custom assertions service that shows the actual diff between expected and actual content
 * when a file comparison fails. The default JUnit5Assertions only says "differs" without 
 * showing what the difference is.
 */
object DiffShowingAssertions : AssertionsService() {

  override fun doesEqualToFile(expectedFile: File, actual: String, sanitizer: (String) -> String): Boolean {
    return JUnit5Assertions.doesEqualToFile(expectedFile, actual, sanitizer)
  }

  override fun assertEqualsToFile(
    expectedFile: File,
    actual: String,
    sanitizer: (String) -> String,
    message: () -> String
  ) {
    if (doesEqualToFile(expectedFile, actual, sanitizer)) return

    val actualText = sanitizer(actual.trim().convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF())
    val expectedText = if (expectedFile.exists()) {
      sanitizer(expectedFile.readText().trim().convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF())
    } else {
      "<file does not exist>"
    }

    val diffMessage = buildString {
      appendLine("${message()}: ${expectedFile.name}")
      appendLine()
      appendLine("To auto-update test expectations, run with: -Doverwrite.output=true")
      appendLine()
      appendLine("═══════════════════════════════════════════════════════════════")
      appendLine("DIFF (expected vs actual):")
      appendLine("───────────────────────────────────────────────────────────────")
      appendLine(computeLineDiff(expectedText, actualText))
    }

    // Use AssertionFailedError without expected/actual to avoid duplication in HTML report
    // (JUnit renders both the message AND expected/actual separately)
    throw AssertionFailedError(diffMessage)
  }

  override fun assertEquals(expected: Any?, actual: Any?, message: (() -> String)?) {
    JUnit5Assertions.assertEquals(expected, actual, message)
  }

  override fun assertNotEquals(expected: Any?, actual: Any?, message: (() -> String)?) {
    JUnit5Assertions.assertNotEquals(expected, actual, message)
  }

  override fun assertTrue(value: Boolean, message: (() -> String)?) {
    JUnit5Assertions.assertTrue(value, message)
  }

  override fun assertFalse(value: Boolean, message: (() -> String)?) {
    JUnit5Assertions.assertFalse(value, message)
  }

  override fun failAll(exceptions: List<Throwable>) {
    JUnit5Assertions.failAll(exceptions)
  }

  override fun assertAll(conditions: List<() -> Unit>) {
    JUnit5Assertions.assertAll(conditions)
  }

  override fun <T> assertSameElements(expected: Collection<T>, actual: Collection<T>, message: (() -> String)?) {
    JUnit5Assertions.assertSameElements(expected, actual, message)
  }

  override fun assertNotNull(value: Any?, message: (() -> String)?) {
    JUnit5Assertions.assertNotNull(value, message)
  }

  override fun fail(message: () -> String): Nothing {
    JUnit5Assertions.fail(message)
  }

  override fun assumeFalse(value: Boolean, message: () -> String) {
    JUnit5Assertions.assumeFalse(value, message)
  }

  /**
   * Computes a simple line-by-line diff showing which lines differ.
   */
  private fun computeLineDiff(expected: String, actual: String): String {
    val expectedLines = expected.lines()
    val actualLines = actual.lines()
    val maxLines = maxOf(expectedLines.size, actualLines.size)

    return buildString {
      var hasDiff = false
      for (i in 0 until maxLines) {
        val expectedLine = expectedLines.getOrNull(i)
        val actualLine = actualLines.getOrNull(i)

        when {
          expectedLine == actualLine -> {
            // Lines match, show context if near a diff
          }
          expectedLine == null -> {
            hasDiff = true
            appendLine("+ Line ${i + 1} (added):   $actualLine")
          }
          actualLine == null -> {
            hasDiff = true
            appendLine("- Line ${i + 1} (removed): $expectedLine")
          }
          else -> {
            hasDiff = true
            appendLine("- Line ${i + 1} (expected): $expectedLine")
            appendLine("+ Line ${i + 1} (actual):   $actualLine")
          }
        }
      }
      if (!hasDiff) {
        appendLine("(No line-level differences found - check whitespace)")
      }
    }
  }
}

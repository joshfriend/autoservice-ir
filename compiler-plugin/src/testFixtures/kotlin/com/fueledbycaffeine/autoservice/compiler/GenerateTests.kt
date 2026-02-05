// Copyright (C) 2024 FueledByCaffeine
// SPDX-License-Identifier: Apache-2.0
package com.fueledbycaffeine.autoservice.compiler

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main() {
  generateTestGroupSuiteWithJUnit5 {
    testGroup(
      testDataRoot = "compiler-plugin/testData",
      testsRoot = "compiler-plugin/test-gen/java",
    ) {
      testClass<AbstractBoxTest> { model("box") }
      testClass<AbstractDiagnosticTest> { model("diagnostic") }
    }
  }
}

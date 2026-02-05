// Copyright (C) 2024 FueledByCaffeine
// SPDX-License-Identifier: Apache-2.0
package com.fueledbycaffeine.autoservice.compiler

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

/**
 * Custom directives for AutoService tests.
 */
object AutoServiceDirectives : SimpleDirectivesContainer() {
  val SERVICES by stringDirective("Expected services to be generated, format: interface=impl1,impl2")
}

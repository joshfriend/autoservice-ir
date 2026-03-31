// Copyright (C) 2024 FueledByCaffeine
// SPDX-License-Identifier: Apache-2.0
package com.fueledbycaffeine.autoservice.compiler

import java.io.File
import java.io.File.pathSeparator
import java.io.File.separator
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

object ClasspathBasedStandardLibrariesPathProvider : KotlinStandardLibrariesPathProvider {
  private val SEP = "\\$separator"

  private val GRADLE_DEPENDENCY =
    (".*?" +
        SEP +
        "(?<name>[^$SEP]*)" +
        SEP +
        "(?<version>[^$SEP]*)" +
        SEP +
        "[^$SEP]*" +
        SEP +
        "\\1-\\2\\.jar")
      .toRegex()

  private val jars =
    System.getProperty("java.class.path")
      .split("\\$pathSeparator".toRegex())
      .dropLastWhile(String::isEmpty)
      .map(::File)
      .associateBy {
        GRADLE_DEPENDENCY.matchEntire(it.path)?.let { it.groups["name"]!!.value } ?: it.name
      }

  private fun getFile(vararg names: String): File {
    return names.firstNotNullOfOrNull(jars::get)
      ?: error("Jar ${names.joinToString(" or ")} not found in classpath:\n${jars.entries.joinToString("\n")}")
  }

  override fun runtimeJarForTests(): File = getFile("kotlin-stdlib")

  override fun runtimeJarForTestsWithJdk8(): File = getFile("kotlin-stdlib-jdk8")

  override fun minimalRuntimeJarForTests(): File = getFile("kotlin-stdlib")

  override fun reflectJarForTests(): File = getFile("kotlin-reflect")

  override fun kotlinTestJarForTests(): File = getFile("kotlin-test")

  override fun scriptRuntimeJarForTests(): File = getFile("kotlin-script-runtime")

  override fun jvmAnnotationsForTests(): File = getFile("kotlin-annotations-jvm")

  override fun getAnnotationsJar(): File = getFile("kotlin-annotations-jvm")

  override fun fullJsStdlib(): File = getFile("kotlin-stdlib-js.klib", "kotlin-stdlib-js")

  override fun defaultJsStdlib(): File = getFile("kotlin-stdlib-js.klib", "kotlin-stdlib-js")

  override fun kotlinTestJsKLib(): File = getFile("kotlin-test-js.klib", "kotlin-test-js")

  override fun fullWasmStdlib(target: WasmTarget): File =
    getFile("kotlin-stdlib-${target.alias}.klib", "kotlin-stdlib-${target.alias}")

  override fun kotlinTestWasmKLib(target: WasmTarget): File =
    getFile("kotlin-test-${target.alias}.klib", "kotlin-test-${target.alias}")

  override fun webStdlibForTests(): File =
    getFile("kotlin-stdlib-wasm-js.klib", "kotlin-stdlib-wasm-js")

  override fun scriptingPluginFilesForTests(): Collection<File> {
    TODO("KT-67573")
  }

  override fun commonStdlibForTests(): File =
    getFile("kotlin-common-stdlib.klib", "kotlin-common-stdlib")
}

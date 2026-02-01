@file:OptIn(ExperimentalCompilerApi::class)

package com.fueledbycaffeine.autoservice

import com.fueledbycaffeine.autoservice.ir.AutoServiceComponentRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

/**
 * Shared test utilities for compiling Kotlin code with the AutoService compiler plugin.
 */
object TestCompilationUtils {

  /**
   * Compiles the given source files with the AutoService compiler plugin.
   *
   * @param sourceFiles The Kotlin source files to compile
   * @return The compilation result containing exit code, output directory, and messages
   */
  fun compile(vararg sourceFiles: SourceFile): JvmCompilationResult {
    return KotlinCompilation().apply {
      sources = sourceFiles.asList()
      compilerPluginRegistrars = listOf(AutoServiceComponentRegistrar())
      // Set kotlin.output.dir system property so our plugin can find it
      System.setProperty("kotlin.output.dir", workingDir.absolutePath)
      inheritClassPath = true
      messageOutputStream = System.out
      verbose = false
    }.compile().also {
      System.clearProperty("kotlin.output.dir")
    }
  }
}

package com.fueledbycaffeine.autoservice.functionaltest.fixtures

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.GradleProject
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import java.io.File

private val gradleVersion: GradleVersion
  get() {
    val versionString = System.getProperty("gradleVersion")
    return if (versionString.isNullOrBlank()) {
      GradleVersion.current()
    } else {
      GradleVersion.version(versionString)
    }
  }
fun GradleProject.build(rootDir: File, vararg args: String): BuildResult =
  GradleBuilder.build(gradleVersion, rootDir, *args, "--info")

fun GradleProject.build(vararg args: String): BuildResult =
  GradleBuilder.build(gradleVersion, rootDir, *args, "--info")

fun GradleProject.buildAndFail(vararg args: String): BuildResult =
  GradleBuilder.buildAndFail(gradleVersion, rootDir, *args, "--info")

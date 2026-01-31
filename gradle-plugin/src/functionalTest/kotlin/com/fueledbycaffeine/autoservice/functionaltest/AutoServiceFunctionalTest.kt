package com.fueledbycaffeine.autoservice.functionaltest

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import com.fueledbycaffeine.autoservice.functionaltest.fixtures.AutoServiceProject
import com.fueledbycaffeine.autoservice.functionaltest.fixtures.build
import com.fueledbycaffeine.autoservice.functionaltest.fixtures.ccReport
import com.fueledbycaffeine.autoservice.functionaltest.fixtures.configurationCacheReused
import com.fueledbycaffeine.autoservice.functionaltest.fixtures.configurationCacheStored
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.jar.JarFile

class AutoServiceFunctionalTest {

  private fun GradleProject.jarFile(): File {
    val libsDir = File(rootDir, "build/libs")
    return libsDir.listFiles()!!.first { it.extension == "jar" }
  }

  private fun GradleProject.assertServiceInJar(serviceName: String, vararg implementations: String) {
    JarFile(jarFile()).use { jar ->
      val serviceEntry = jar.getEntry("META-INF/services/$serviceName")
      assertThat(serviceEntry).isNotNull()

      val content = jar.getInputStream(serviceEntry).bufferedReader().readText()
      implementations.forEach { impl ->
        assertThat(content).contains(impl)
      }
    }
  }

  @Test
  fun `generates service files with inferred type`() {
    // Given
    val project = AutoServiceProject().simpleInferred()

    // When
    val result = project.build("jar")

    // Then
    assertThat(result).task(":jar").succeeded()
    project.assertServiceInJar("test.MyService", "test.MyServiceImpl")
    assertThat(result.configurationCacheStored).isTrue()
    val report = result.ccReport()
    assertThat(report.totalProblemCount).isEqualTo(0)
    assertThat(report.inputs).isEmpty()
  }

  @Test
  fun `generates service files with explicit type`() {
    // Given
    val project = AutoServiceProject().explicitType()

    // When
    val result = project.build("jar")

    // Then
    assertThat(result).task(":jar").succeeded()
    project.assertServiceInJar("test.Logger", "test.ConsoleLogger")
    assertThat(result.configurationCacheStored).isTrue()
  }

  @Test
  fun `generates service files for multiple interfaces`() {
    // Given
    val project = AutoServiceProject().multipleInterfaces()

    // When
    val result = project.build("jar")

    // Then
    assertThat(result).task(":jar").succeeded()
    project.assertServiceInJar("test.ServiceA", "test.MultiServiceImpl")
    project.assertServiceInJar("test.ServiceB", "test.MultiServiceImpl")
    assertThat(result.configurationCacheStored).isTrue()
  }

  @Test
  fun `generates service files for multiple implementations`() {
    // Given
    val project = AutoServiceProject().multipleImplementations()

    // When
    val result = project.build("jar")

    // Then
    assertThat(result).task(":jar").succeeded()
    project.assertServiceInJar("test.Logger", "test.ConsoleLogger", "test.FileLogger")
    assertThat(result.configurationCacheStored).isTrue()
  }

  @Test
  fun `works with Google annotation`() {
    // Given
    val project = AutoServiceProject().withGoogleAnnotation()

    // When
    val result = project.build("jar")

    // Then
    assertThat(result).task(":jar").succeeded()
    project.assertServiceInJar("test.Logger", "test.GoogleLogger")
    assertThat(result.configurationCacheStored).isTrue()
  }

  @Test
  fun `supports mixing both annotations`() {
    // Given
    val project = AutoServiceProject().mixedAnnotations()

    // When
    val result = project.build("jar")

    // Then
    assertThat(result).task(":jar").succeeded()
    project.assertServiceInJar("test.Logger", "test.OurLogger", "test.GoogleLogger")
    assertThat(result.configurationCacheStored).isTrue()
  }

  @Test
  fun `clean build works correctly`() {
    // Given
    val project = AutoServiceProject().simpleInferred()

    // When
    val firstResult = project.build("jar")
    project.build("clean")
    val cleanResult = project.build("jar")

    // Then
    assertThat(firstResult).task(":jar").succeeded()
    assertThat(cleanResult).task(":jar").succeeded()
    project.assertServiceInJar("test.MyService", "test.MyServiceImpl")
    assertThat(firstResult.configurationCacheStored).isTrue()
    assertThat(cleanResult.configurationCacheReused).isTrue()
  }
}

package com.fueledbycaffeine.autoservice.functionaltest

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import com.fueledbycaffeine.autoservice.functionaltest.fixtures.AutoServiceProject
import com.fueledbycaffeine.autoservice.functionaltest.fixtures.build
import com.fueledbycaffeine.autoservice.functionaltest.fixtures.buildAndFail
import com.fueledbycaffeine.autoservice.functionaltest.fixtures.configurationCacheReused
import com.fueledbycaffeine.autoservice.functionaltest.fixtures.configurationCacheStored
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.jar.JarFile

/**
 * Tests incremental compilation scenarios where AutoService annotations are modified.
 */
class IncrementalCompilationTest {

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

  private fun GradleProject.assertServiceNotInJar(serviceName: String, implementation: String) {
    JarFile(jarFile()).use { jar ->
      val serviceEntry = jar.getEntry("META-INF/services/$serviceName")
      if (serviceEntry != null) {
        val content = jar.getInputStream(serviceEntry).bufferedReader().readText()
        assertThat(content).doesNotContain(implementation)
      }
    }
  }

  private fun GradleProject.assertNoServiceInJar(serviceName: String) {
    JarFile(jarFile()).use { jar ->
      val serviceEntry = jar.getEntry("META-INF/services/$serviceName")
      assertThat(serviceEntry).isNull()
    }
  }

  @Test
  fun `adding AutoService annotation to existing class generates service file`() {
    // Given - Start with a class without @AutoService
    val project = AutoServiceProject().simpleInferred()
    val implFile = File(project.rootDir, "src/main/kotlin/test/MyServiceImpl.kt")
    
    // Write version without annotation
    implFile.writeText("""
      package test
      
      class MyServiceImpl : MyService {
        override fun execute() = println("MyServiceImpl")
      }
    """.trimIndent())

    // When - First build without annotation
    val firstBuild = project.build("jar")
    assertThat(firstBuild).task(":jar").succeeded()
    assertThat(firstBuild.configurationCacheStored).isTrue()
    project.assertNoServiceInJar("test.MyService")
    
    // Then - Add @AutoService annotation
    implFile.writeText("""
      package test
      
      import com.fueledbycaffeine.autoservice.AutoService
      
      @AutoService
      class MyServiceImpl : MyService {
        override fun execute() = println("MyServiceImpl")
      }
    """.trimIndent())
    
    val secondBuild = project.build("jar")  // Incremental build
    assertThat(secondBuild).task(":jar").succeeded()
    assertThat(secondBuild.configurationCacheReused).isTrue()
    project.assertServiceInJar("test.MyService", "test.MyServiceImpl")
  }

  @Test
  fun `removing AutoService annotation removes class from service file`() {
    // Given - Start with two annotated classes
    val project = AutoServiceProject().multipleImplementations()
    
    // When - First build with both implementations
    val firstBuild = project.build("jar")
    assertThat(firstBuild).task(":jar").succeeded()
    assertThat(firstBuild.configurationCacheStored).isTrue()
    project.assertServiceInJar("test.Logger", "test.ConsoleLogger", "test.FileLogger")
    
    // Then - Remove annotation from one implementation
    val consoleLoggerFile = File(project.rootDir, "src/main/kotlin/test/ConsoleLogger.kt")
    consoleLoggerFile.writeText("""
      package test
      
      // Annotation removed!
      class ConsoleLogger : Logger {
        override fun log(message: String) = println(message)
      }
    """.trimIndent())
    
    val secondBuild = project.build("jar")  // Incremental build
    assertThat(secondBuild).task(":jar").succeeded()
    assertThat(secondBuild.configurationCacheReused).isTrue()
    project.assertServiceNotInJar("test.Logger", "test.ConsoleLogger")
    project.assertServiceInJar("test.Logger", "test.FileLogger")
  }

  @Test
  fun `changing from inferred to explicit type works`() {
    // Given - Start with inferred type
    val project = AutoServiceProject().simpleInferred()
    
    // When - Build with inferred type
    val firstBuild = project.build("jar")
    assertThat(firstBuild).task(":jar").succeeded()
    assertThat(firstBuild.configurationCacheStored).isTrue()
    project.assertServiceInJar("test.MyService", "test.MyServiceImpl")
    
    // Then - Change to explicit type
    val implFile = File(project.rootDir, "src/main/kotlin/test/MyServiceImpl.kt")
    implFile.writeText("""
      package test
      
      import com.fueledbycaffeine.autoservice.AutoService
      
      @AutoService(MyService::class)  // Now explicit
      class MyServiceImpl : MyService {
        override fun execute() = println("MyServiceImpl")
      }
    """.trimIndent())
    
    val secondBuild = project.build("jar")  // Incremental build
    assertThat(secondBuild).task(":jar").succeeded()
    assertThat(secondBuild.configurationCacheReused).isTrue()
    project.assertServiceInJar("test.MyService", "test.MyServiceImpl")
  }

  @Test
  fun `adding second service interface parameter works`() {
    // Given - Start with single interface
    val project = AutoServiceProject().explicitType()
    
    // When - Build with single interface
    val firstBuild = project.build("jar")
    assertThat(firstBuild).task(":jar").succeeded()
    assertThat(firstBuild.configurationCacheStored).isTrue()
    project.assertServiceInJar("test.Logger", "test.ConsoleLogger")
    
    // Add a second interface
    File(project.rootDir, "src/main/kotlin/test/Cache.kt").writeText("""
      package test
      
      interface Cache {
        fun get(key: String): String?
      }
    """.trimIndent())
    
    // Then - Modify implementation to implement both interfaces
    val implFile = File(project.rootDir, "src/main/kotlin/test/ConsoleLogger.kt")
    implFile.writeText("""
      package test
      
      import com.fueledbycaffeine.autoservice.AutoService
      
      @AutoService(Logger::class, Cache::class)
      class ConsoleLogger : Logger, Cache {
        override fun log(message: String) = println(message)
        override fun get(key: String): String? = null
      }
    """.trimIndent())
    
    val secondBuild = project.build("jar")  // Incremental build
    assertThat(secondBuild).task(":jar").succeeded()
    assertThat(secondBuild.configurationCacheReused).isTrue()
    project.assertServiceInJar("test.Logger", "test.ConsoleLogger")
    project.assertServiceInJar("test.Cache", "test.ConsoleLogger")
  }

  @Test
  fun `removing service interface parameter removes from service file`() {
    // Given - Start with multiple interfaces
    val project = AutoServiceProject().multipleInterfaces()
    
    // When - Build with both interfaces
    val firstBuild = project.build("jar")
    assertThat(firstBuild).task(":jar").succeeded()
    assertThat(firstBuild.configurationCacheStored).isTrue()
    project.assertServiceInJar("test.ServiceA", "test.MultiServiceImpl")
    project.assertServiceInJar("test.ServiceB", "test.MultiServiceImpl")
    
    // Then - Change annotation to only register for ServiceA
    val implFile = File(project.rootDir, "src/main/kotlin/test/MultiServiceImpl.kt")
    implFile.writeText("""
      package test
      
      import com.fueledbycaffeine.autoservice.AutoService
      
      @AutoService(ServiceA::class)  // Only ServiceA now
      class MultiServiceImpl : ServiceA, ServiceB {
        override fun doA() {}
        override fun doB() {}
      }
    """.trimIndent())
    
    val secondBuild = project.build("jar")  // Incremental build
    assertThat(secondBuild).task(":jar").succeeded()
    assertThat(secondBuild.configurationCacheReused).isTrue()
    project.assertServiceInJar("test.ServiceA", "test.MultiServiceImpl")
    project.assertServiceNotInJar("test.ServiceB", "test.MultiServiceImpl")
  }

  @Test
  fun `switching from Google annotation to ours works`() {
    // Given - Start with Google's annotation
    val project = AutoServiceProject().withGoogleAnnotation()
    
    // When - Build with Google's annotation
    val firstBuild = project.build("jar")
    assertThat(firstBuild).task(":jar").succeeded()
    assertThat(firstBuild.configurationCacheStored).isTrue()
    project.assertServiceInJar("test.Logger", "test.GoogleLogger")
    
    // Then - Switch to our annotation with inference
    val implFile = File(project.rootDir, "src/main/kotlin/test/GoogleLogger.kt")
    implFile.writeText("""
      package test
      
      import com.fueledbycaffeine.autoservice.AutoService
      
      @AutoService  // Switched to our annotation with inference
      class GoogleLogger : Logger {
        override fun log(message: String) = println(message)
      }
    """.trimIndent())
    
    val secondBuild = project.build("jar")  // Incremental build
    assertThat(secondBuild).task(":jar").succeeded()
    assertThat(secondBuild.configurationCacheReused).isTrue()
    project.assertServiceInJar("test.Logger", "test.GoogleLogger")
  }

  @Test
  fun `adding new implementation updates service file`() {
    // Given - Start with one implementation
    val project = AutoServiceProject().simpleInferred()
    
    // When - Initial build
    val firstBuild = project.build("jar")
    assertThat(firstBuild).task(":jar").succeeded()
    assertThat(firstBuild.configurationCacheStored).isTrue()
    project.assertServiceInJar("test.MyService", "test.MyServiceImpl")
    
    // Then - Add a second implementation
    File(project.rootDir, "src/main/kotlin/test/AnotherServiceImpl.kt").writeText("""
      package test
      
      import com.fueledbycaffeine.autoservice.AutoService
      
      @AutoService
      class AnotherServiceImpl : MyService {
        override fun execute() = println("AnotherServiceImpl")
      }
    """.trimIndent())
    
    val secondBuild = project.build("jar")  // Incremental build
    assertThat(secondBuild).task(":jar").succeeded()
    assertThat(secondBuild.configurationCacheReused).isTrue()
    project.assertServiceInJar("test.MyService", "test.MyServiceImpl", "test.AnotherServiceImpl")
  }

  @Test
  fun `deleting implementation class removes it from service file`() {
    // Given - Start with multiple implementations
    val project = AutoServiceProject().multipleImplementations()
    
    // When - Build with both
    val firstBuild = project.build("jar")
    assertThat(firstBuild).task(":jar").succeeded()
    assertThat(firstBuild.configurationCacheStored).isTrue()
    project.assertServiceInJar("test.Logger", "test.ConsoleLogger", "test.FileLogger")
    
    // Then - Delete one implementation
    val fileLoggerFile = File(project.rootDir, "src/main/kotlin/test/FileLogger.kt")
    fileLoggerFile.delete()
    
    // Incremental build (not clean)
    // With the FIR mirror class, deleting the source file triggers IC which:
    // 1. Deletes the stale class files (FileLogger.class, FileLogger$__AutoService__.class)
    // 2. Runs our IR extension which validates service files and removes stale entries
    val secondBuild = project.build("jar")
    assertThat(secondBuild).task(":jar").succeeded()
    assertThat(secondBuild.configurationCacheReused).isTrue()
    project.assertServiceInJar("test.Logger", "test.ConsoleLogger")
    project.assertServiceNotInJar("test.Logger", "test.FileLogger")
  }

  @Test
  fun `incremental build preserves service files`() {
    // Given
    val project = AutoServiceProject().multipleImplementations()

    // When
    val firstBuild = project.build("jar")
    assertThat(firstBuild).task(":jar").succeeded()
    assertThat(firstBuild.configurationCacheStored).isTrue()
    project.assertServiceInJar("test.Logger", "test.ConsoleLogger", "test.FileLogger")
    
    // Add a new source file
    val newImpl = File(project.rootDir, "src/main/kotlin/test/NetworkLogger.kt")
    newImpl.parentFile.mkdirs()
    newImpl.writeText(
      """
        package test
        
        import com.fueledbycaffeine.autoservice.AutoService
        
        @AutoService
        class NetworkLogger : Logger {
          override fun log(message: String) {
            // Send to network
          }
        }
      """.trimIndent()
    )
    
    // Incremental build (not clean)
    val secondBuild = project.build("jar")
    assertThat(secondBuild).task(":jar").succeeded()
    assertThat(secondBuild.configurationCacheReused).isTrue()
    project.assertServiceInJar("test.Logger", "test.ConsoleLogger", "test.FileLogger", "test.NetworkLogger")
  }

  @Test
  fun `changing supertype triggers service file update`() {
    // Given - Start with a class implementing one interface
    val project = AutoServiceProject().simpleInferred()
    
    // When - Initial build
    val firstBuild = project.build("jar")
    assertThat(firstBuild).task(":jar").succeeded()
    assertThat(firstBuild.configurationCacheStored).isTrue()
    project.assertServiceInJar("test.MyService", "test.MyServiceImpl")
    
    // Then - Change the interface (supertype change)
    // Note: Supertype changes don't currently trigger IC in Kotlin, but our IR hook should handle it
    val interfaceFile = File(project.rootDir, "src/main/kotlin/test/MyService.kt")
    interfaceFile.writeText("""
      package test
      
      interface MyService {
        fun execute()
        fun newMethod()  // Added new method - breaking change
      }
    """.trimIndent())
    
    // Update implementation to match
    val implFile = File(project.rootDir, "src/main/kotlin/test/MyServiceImpl.kt")
    implFile.writeText("""
      package test
      
      import com.fueledbycaffeine.autoservice.AutoService
      
      @AutoService
      class MyServiceImpl : MyService {
        override fun execute() = println("MyServiceImpl")
        override fun newMethod() = println("new method")
      }
    """.trimIndent())
    
    val secondBuild = project.build("jar")
    assertThat(secondBuild).task(":jar").succeeded()
    assertThat(secondBuild.configurationCacheReused).isTrue()
    project.assertServiceInJar("test.MyService", "test.MyServiceImpl")
  }

  @Test
  fun `removing supertype with inferred type reports error and skips registration`() {
    // Given - Class with inferred type from supertype
    val project = AutoServiceProject().simpleInferred()
    
    // When - Initial build succeeds
    val firstBuild = project.build("jar")
    assertThat(firstBuild).task(":jar").succeeded()
    assertThat(firstBuild.configurationCacheStored).isTrue()
    project.assertServiceInJar("test.MyService", "test.MyServiceImpl")
    
    // Then - Remove the supertype but keep @AutoService with no parameters
    // This is INVALID - @AutoService with no parameters requires a supertype
    val implFile = File(project.rootDir, "src/main/kotlin/test/MyServiceImpl.kt")
    implFile.writeText("""
      package test
      
      import com.fueledbycaffeine.autoservice.AutoService
      
      @AutoService  // ERROR: No parameters and no supertype!
      class MyServiceImpl {
        fun execute() = println("MyServiceImpl")
      }
    """.trimIndent())
    
    // Build should fail with an error about missing service interfaces
    val secondBuild = project.buildAndFail("jar")
    assertThat(secondBuild).task(":compileKotlin").failed()
    assertThat(secondBuild.output).contains("No service interfaces provided for element")
  }

  @Test
  fun `changing to different supertype updates service file`() {
    // Given - Class implementing one service
    val project = AutoServiceProject().simpleInferred()
    
    // When - Initial build
    val firstBuild = project.build("jar")
    assertThat(firstBuild).task(":jar").succeeded()
    assertThat(firstBuild.configurationCacheStored).isTrue()
    project.assertServiceInJar("test.MyService", "test.MyServiceImpl")
    
    // Then - Change to implement a different interface
    // Add new interface
    File(project.rootDir, "src/main/kotlin/test/OtherService.kt").writeText("""
      package test
      
      interface OtherService {
        fun doOther()
      }
    """.trimIndent())
    
    // Change implementation to implement different interface
    val implFile = File(project.rootDir, "src/main/kotlin/test/MyServiceImpl.kt")
    implFile.writeText("""
      package test
      
      import com.fueledbycaffeine.autoservice.AutoService
      
      @AutoService
      class MyServiceImpl : OtherService {
        override fun doOther() = println("MyServiceImpl")
      }
    """.trimIndent())
    
    val secondBuild = project.build("jar")
    assertThat(secondBuild).task(":jar").succeeded()
    assertThat(secondBuild.configurationCacheReused).isTrue()
    project.assertServiceNotInJar("test.MyService", "test.MyServiceImpl")
    project.assertServiceInJar("test.OtherService", "test.MyServiceImpl")
  }
}

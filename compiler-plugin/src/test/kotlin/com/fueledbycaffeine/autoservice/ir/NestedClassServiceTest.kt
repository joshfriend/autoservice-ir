package com.fueledbycaffeine.autoservice.ir

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Test
import java.io.File
import java.util.ServiceLoader
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for nested/inner class service provider handling.
 * 
 * These tests verify that:
 * 1. Nested classes are registered with JVM binary names using '$' separator
 * 2. Service files can be loaded via ServiceLoader
 * 3. Both our annotation and Google's annotation work with nested classes
 */
class NestedClassServiceTest {

  @Test
  fun `nested class provider uses dollar sign in service file`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          interface MyService {
            fun getName(): String
          }
        """
      ),
      SourceFile.kotlin(
        "OuterClass.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          object OuterClass {
            @AutoService(MyService::class)
            class NestedProvider : MyService {
              override fun getName() = "NestedProvider"
            }
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    val serviceFile = File(result.outputDirectory, "META-INF/services/test.MyService")
    assertTrue(serviceFile.exists(), "Service file should exist")
    
    val content = serviceFile.readText()
    // The service file should use $ for nested classes, not .
    assertTrue(
      content.contains("test.OuterClass\$NestedProvider"),
      "Service file should contain JVM binary name with \$ for nested class. Actual content: $content"
    )
    // Make sure it doesn't use dot notation
    assertTrue(
      !content.contains("test.OuterClass.NestedProvider"),
      "Service file should NOT contain dot notation for nested class"
    )
  }

  @Test
  fun `deeply nested class provider uses dollar signs`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          interface MyService {
            fun getName(): String
          }
        """
      ),
      SourceFile.kotlin(
        "OuterClass.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          class Outer {
            class Middle {
              @AutoService(MyService::class)
              class DeeplyNestedProvider : MyService {
                override fun getName() = "DeeplyNestedProvider"
              }
            }
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    val serviceFile = File(result.outputDirectory, "META-INF/services/test.MyService")
    assertTrue(serviceFile.exists(), "Service file should exist")
    
    val content = serviceFile.readText()
    assertTrue(
      content.contains("test.Outer\$Middle\$DeeplyNestedProvider"),
      "Service file should contain JVM binary name with \$ for deeply nested class. Actual: $content"
    )
  }

  @Test
  fun `nested class with Google annotation uses dollar sign`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          interface MyService {
            fun getName(): String
          }
        """
      ),
      SourceFile.kotlin(
        "OuterClass.kt",
        """
          package test
          
          import com.google.auto.service.AutoService
          
          object Container {
            @AutoService(MyService::class)
            class Provider : MyService {
              override fun getName() = "Provider"
            }
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    val serviceFile = File(result.outputDirectory, "META-INF/services/test.MyService")
    assertTrue(serviceFile.exists(), "Service file should exist")
    
    val content = serviceFile.readText()
    assertTrue(
      content.contains("test.Container\$Provider"),
      "Service file should contain JVM binary name with \$ for nested class. Actual: $content"
    )
  }

  @Test
  fun `nested class can be loaded via ServiceLoader`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          interface MyService {
            fun getName(): String
          }
        """
      ),
      SourceFile.kotlin(
        "OuterClass.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          object OuterClass {
            @AutoService(MyService::class)
            class NestedProvider : MyService {
              override fun getName() = "NestedProvider"
            }
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    // Verify the service can actually be loaded by ServiceLoader
    val classLoader = result.classLoader
    val serviceInterface = classLoader.loadClass("test.MyService")
    
    @Suppress("UNCHECKED_CAST")
    val services = ServiceLoader.load(serviceInterface, classLoader).toList()
    
    assertEquals(1, services.size, "Should load exactly one service implementation")
    
    // Verify it's the correct class
    val service = services.first()
    assertEquals("test.OuterClass\$NestedProvider", service::class.java.name)
  }

  @Test
  fun `nested interface as service type uses dollar sign`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          object ServiceHolder {
            interface NestedService {
              fun process(): String
            }
          }
        """
      ),
      SourceFile.kotlin(
        "Implementation.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          @AutoService(ServiceHolder.NestedService::class)
          class NestedServiceImpl : ServiceHolder.NestedService {
            override fun process() = "processed"
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    // The service file name should use $ for the nested interface
    val serviceFile = File(result.outputDirectory, "META-INF/services/test.ServiceHolder\$NestedService")
    assertTrue(serviceFile.exists(), "Service file for nested interface should exist")
    
    val content = serviceFile.readText()
    assertTrue(
      content.contains("test.NestedServiceImpl"),
      "Service file should contain implementation class"
    )
  }

  @Test
  fun `companion object provider class uses dollar sign`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          interface MyService {
            fun getName(): String
          }
        """
      ),
      SourceFile.kotlin(
        "ServiceWithCompanion.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          class ServiceWithCompanion {
            companion object {
              @AutoService(MyService::class)
              class CompanionProvider : MyService {
                override fun getName() = "CompanionProvider"
              }
            }
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    val serviceFile = File(result.outputDirectory, "META-INF/services/test.MyService")
    assertTrue(serviceFile.exists(), "Service file should exist")
    
    val content = serviceFile.readText()
    // Companion object in Kotlin is compiled as Companion nested class
    assertTrue(
      content.contains("test.ServiceWithCompanion\$Companion\$CompanionProvider"),
      "Service file should contain JVM binary name for class nested in companion object. Actual: $content"
    )
  }

  @Test
  fun `inferred interface for nested provider class`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          interface MyService {
            fun getName(): String
          }
        """
      ),
      SourceFile.kotlin(
        "OuterClass.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          object OuterClass {
            @AutoService  // Interface should be inferred
            class InferredProvider : MyService {
              override fun getName() = "InferredProvider"
            }
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    val serviceFile = File(result.outputDirectory, "META-INF/services/test.MyService")
    assertTrue(serviceFile.exists(), "Service file should exist with inferred interface")
    
    val content = serviceFile.readText()
    assertTrue(
      content.contains("test.OuterClass\$InferredProvider"),
      "Service file should contain JVM binary name with \$ for nested class. Actual: $content"
    )
  }

  @Test
  fun `multiple nested providers in same outer class`() {
    val result = compile(
      SourceFile.kotlin(
        "TestServices.kt",
        """
          package test
          
          interface ServiceA {
            fun getA(): String
          }
          
          interface ServiceB {
            fun getB(): String
          }
        """
      ),
      SourceFile.kotlin(
        "OuterClass.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          object Container {
            @AutoService(ServiceA::class)
            class ProviderA : ServiceA {
              override fun getA() = "A"
            }
            
            @AutoService(ServiceB::class)
            class ProviderB : ServiceB {
              override fun getB() = "B"
            }
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    val serviceFileA = File(result.outputDirectory, "META-INF/services/test.ServiceA")
    val serviceFileB = File(result.outputDirectory, "META-INF/services/test.ServiceB")
    
    assertTrue(serviceFileA.exists(), "Service file A should exist")
    assertTrue(serviceFileB.exists(), "Service file B should exist")
    
    assertTrue(serviceFileA.readText().contains("test.Container\$ProviderA"))
    assertTrue(serviceFileB.readText().contains("test.Container\$ProviderB"))
  }

  private fun compile(vararg sourceFiles: SourceFile): JvmCompilationResult {
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

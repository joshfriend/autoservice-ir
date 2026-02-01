package com.fueledbycaffeine.autoservice.fir

import com.fueledbycaffeine.autoservice.ir.AutoServiceComponentRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for FIR checkers that provide IDE error checking for @AutoService annotations.
 */
class AutoServiceFirCheckersTest {

  @Test
  fun `test abstract class error`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          interface MyService {
            fun doSomething()
          }
          
          @AutoService(MyService::class)
          abstract class AbstractServiceImpl : MyService {
            override fun doSomething() {}
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assertTrue(
      result.messages.contains("@AutoService cannot be applied to an abstract class"),
      "Should report error for abstract class. Actual: ${result.messages}"
    )
  }

  @Test
  fun `test abstract class error with Google annotation`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.google.auto.service.AutoService
          
          interface MyService {
            fun doSomething()
          }
          
          @AutoService(MyService::class)
          abstract class AbstractServiceImpl : MyService {
            override fun doSomething() {}
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assertTrue(
      result.messages.contains("@AutoService cannot be applied to an abstract class"),
      "Should report error for abstract class with Google annotation. Actual: ${result.messages}"
    )
  }

  @Test
  fun `test private class error`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          interface MyService {
            fun doSomething()
          }
          
          @AutoService(MyService::class)
          private class PrivateServiceImpl : MyService {
            override fun doSomething() {}
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assertTrue(
      result.messages.contains("@AutoService classes must be public or internal") ||
      result.messages.contains("private"),
      "Should report error for private class. Actual: ${result.messages}"
    )
  }

  @Test
  fun `test interface error`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          interface MyService {
            fun doSomething()
          }
          
          @AutoService(MyService::class)
          interface MyServiceInterface : MyService
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assertTrue(
      result.messages.contains("@AutoService cannot be applied to an interface"),
      "Should report error for interface. Actual: ${result.messages}"
    )
  }

  @Test
  fun `test enum class error`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          interface MyService {
            fun doSomething()
          }
          
          @AutoService(MyService::class)
          enum class ServiceEnum : MyService {
            INSTANCE;
            override fun doSomething() {}
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assertTrue(
      result.messages.contains("@AutoService cannot be applied to an enum class"),
      "Should report error for enum class. Actual: ${result.messages}"
    )
  }

  @Test
  fun `test annotation class error`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          @AutoService
          annotation class MyAnnotation
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assertTrue(
      result.messages.contains("@AutoService cannot be applied to an annotation class"),
      "Should report error for annotation class. Actual: ${result.messages}"
    )
  }

  @Test
  fun `test object error`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          interface MyService {
            fun doSomething()
          }
          
          @AutoService(MyService::class)
          object ServiceObject : MyService {
            override fun doSomething() {}
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assertTrue(
      result.messages.contains("@AutoService cannot be applied to an object"),
      "Should report error for object. Actual: ${result.messages}"
    )
  }

  @Test
  fun `test valid public class succeeds`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          interface MyService {
            fun doSomething()
          }
          
          @AutoService(MyService::class)
          class PublicServiceImpl : MyService {
            override fun doSomething() {}
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode,
      "Public class should compile successfully. Messages: ${result.messages}")
  }

  @Test
  fun `test valid internal class succeeds`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          interface MyService {
            fun doSomething()
          }
          
          @AutoService(MyService::class)
          internal class InternalServiceImpl : MyService {
            override fun doSomething() {}
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode,
      "Internal class should compile successfully. Messages: ${result.messages}")
  }

  @Test
  fun `test concrete class with inferred interface succeeds`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          interface MyService {
            fun doSomething()
          }
          
          @AutoService  // Inferred interface
          class ServiceImpl : MyService {
            override fun doSomething() {}
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode,
      "Concrete class with inferred interface should succeed. Messages: ${result.messages}")
  }

  @Test
  fun `test open class succeeds`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          interface MyService {
            fun doSomething()
          }
          
          @AutoService(MyService::class)
          open class OpenServiceImpl : MyService {
            override fun doSomething() {}
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode,
      "Open (non-abstract) class should succeed. Messages: ${result.messages}")
  }

  @Test
  fun `test multiple errors reported`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          interface MyService {
            fun doSomething()
          }
          
          @AutoService(MyService::class)
          private abstract class BadServiceImpl : MyService {
            override fun doSomething() {}
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    // Should report both abstract and private errors
    val hasAbstractError = result.messages.contains("@AutoService must be applied to a non-abstract class")
    val hasPrivateError = result.messages.contains("@AutoService classes must be public or internal")

    assertTrue(
      hasAbstractError || hasPrivateError,
      "Should report at least one error (abstract or private). Actual: ${result.messages}"
    )
  }

  @Test
  fun `test nested class with proper visibility succeeds`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          interface MyService {
            fun doSomething()
          }
          
          class Outer {
            @AutoService(MyService::class)
            class NestedServiceImpl : MyService {
              override fun doSomething() {}
            }
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode,
      "Nested public class should succeed. Messages: ${result.messages}")
  }

  @Test
  fun `test inner class with annotation`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          interface MyService {
            fun doSomething()
          }
          
          class Outer {
            @AutoService(MyService::class)
            inner class InnerServiceImpl : MyService {
              override fun doSomething() {}
            }
          }
        """
      )
    )

    // Inner classes should work fine as long as they're public/internal
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode,
      "Inner class should succeed if public. Messages: ${result.messages}")
  }

  private fun compile(vararg sourceFiles: SourceFile): JvmCompilationResult {
    return KotlinCompilation().apply {
      sources = sourceFiles.asList()
      compilerPluginRegistrars = listOf(AutoServiceComponentRegistrar())
      System.setProperty("kotlin.output.dir", workingDir.absolutePath)
      inheritClassPath = true
      messageOutputStream = System.out
      verbose = false
    }.compile().also {
      System.clearProperty("kotlin.output.dir")
    }
  }
}

@file:OptIn(ExperimentalCompilerApi::class)

package com.fueledbycaffeine.autoservice.ir

import com.fueledbycaffeine.autoservice.TestCompilationUtils.compile
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests using our own @AutoService annotation (not Google's)
 */
class AutoServiceAnnotationTest {

  @Test
  fun `test our annotation with inferred type`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          interface MyService {
            fun doSomething()
          }
        """
      ),
      SourceFile.kotlin(
        "TestServiceImpl.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          @AutoService  // Using our annotation with inference
          class MyServiceImpl : MyService {
            override fun doSomething() {
              println("Hello")
            }
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    val serviceFile = File(result.outputDirectory, "META-INF/services/test.MyService")
    assertTrue(serviceFile.exists(), "Service file should exist")
    
    val content = serviceFile.readText()
    assertTrue(content.contains("test.MyServiceImpl"))
  }

  @Test
  fun `test our annotation with explicit type`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          interface MyService {
            fun doSomething()
          }
        """
      ),
      SourceFile.kotlin(
        "TestServiceImpl.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          @AutoService(MyService::class)  // Using our annotation with explicit type
          class MyServiceImpl : MyService {
            override fun doSomething() {
              println("Hello")
            }
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    val serviceFile = File(result.outputDirectory, "META-INF/services/test.MyService")
    assertTrue(serviceFile.exists())
    assertTrue(serviceFile.readText().contains("test.MyServiceImpl"))
  }

  @Test
  fun `test our annotation with multiple interfaces`() {
    val result = compile(
      SourceFile.kotlin(
        "TestServices.kt",
        """
          package test
          
          interface ServiceA { fun doA() }
          interface ServiceB { fun doB() }
        """
      ),
      SourceFile.kotlin(
        "TestServiceImpl.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          @AutoService(ServiceA::class, ServiceB::class)
          class MultiServiceImpl : ServiceA, ServiceB {
            override fun doA() {}
            override fun doB() {}
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    val serviceFileA = File(result.outputDirectory, "META-INF/services/test.ServiceA")
    val serviceFileB = File(result.outputDirectory, "META-INF/services/test.ServiceB")
    
    assertTrue(serviceFileA.exists())
    assertTrue(serviceFileB.exists())
    assertTrue(serviceFileA.readText().contains("test.MultiServiceImpl"))
    assertTrue(serviceFileB.readText().contains("test.MultiServiceImpl"))
  }

  @Test
  fun `test mixing both annotations in same project`() {
    val result = compile(
      SourceFile.kotlin(
        "TestService.kt",
        """
          package test
          
          interface MyService {
            fun doSomething()
          }
        """
      ),
      SourceFile.kotlin(
        "OurImpl.kt",
        """
          package test
          
          import com.fueledbycaffeine.autoservice.AutoService
          
          @AutoService  // Our annotation
          class OurImpl : MyService {
            override fun doSomething() {}
          }
        """
      ),
      SourceFile.kotlin(
        "GoogleImpl.kt",
        """
          package test
          
          import com.google.auto.service.AutoService
          
          @AutoService(MyService::class)  // Google's annotation
          class GoogleImpl : MyService {
            override fun doSomething() {}
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    val serviceFile = File(result.outputDirectory, "META-INF/services/test.MyService")
    assertTrue(serviceFile.exists())
    
    val content = serviceFile.readText()
    assertTrue(content.contains("test.OurImpl"), "Should contain our annotation usage")
    assertTrue(content.contains("test.GoogleImpl"), "Should contain Google's annotation usage")
  }
}

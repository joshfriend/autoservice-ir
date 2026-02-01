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

class AutoServiceCompilerPluginTest {

  @Test
  fun `test single service implementation`() {
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
          
          import com.google.auto.service.AutoService
          
          @AutoService(MyService::class)
          class MyServiceImpl : MyService {
            override fun doSomething() {
              println("Hello from MyServiceImpl")
            }
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    // Check that the service file was generated
    val serviceFile = File(result.outputDirectory, "META-INF/services/test.MyService")
    assertTrue(serviceFile.exists(), "Service file should exist")
    
    val content = serviceFile.readText()
    assertTrue(content.contains("test.MyServiceImpl"), "Service file should contain implementation class")
  }

  @Test
  fun `test multiple service implementations`() {
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
        "TestServiceImpl1.kt",
        """
          package test
          
          import com.google.auto.service.AutoService
          
          @AutoService(MyService::class)
          class MyServiceImpl1 : MyService {
            override fun doSomething() {}
          }
        """
      ),
      SourceFile.kotlin(
        "TestServiceImpl2.kt",
        """
          package test
          
          import com.google.auto.service.AutoService
          
          @AutoService(MyService::class)
          class MyServiceImpl2 : MyService {
            override fun doSomething() {}
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    val serviceFile = File(result.outputDirectory, "META-INF/services/test.MyService")
    assertTrue(serviceFile.exists())
    
    val content = serviceFile.readText()
    assertTrue(content.contains("test.MyServiceImpl1"))
    assertTrue(content.contains("test.MyServiceImpl2"))
  }

  @Test
  fun `test inferred service interface`() {
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
          
          import com.google.auto.service.AutoService
          
          @AutoService
          class MyServiceImpl : MyService {
            override fun doSomething() {
              println("Hello from MyServiceImpl with inferred interface")
            }
          }
        """
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    
    // Check that the service file was generated
    val serviceFile = File(result.outputDirectory, "META-INF/services/test.MyService")
    assertTrue(serviceFile.exists(), "Service file should exist for inferred interface")
    
    val content = serviceFile.readText()
    assertTrue(content.contains("test.MyServiceImpl"), "Service file should contain implementation class")
  }

  @Test
  fun `test multiple service interfaces`() {
    val result = compile(
      SourceFile.kotlin(
        "TestServices.kt",
        """
          package test
          
          interface ServiceA {
            fun doA()
          }
          
          interface ServiceB {
            fun doB()
          }
        """
      ),
      SourceFile.kotlin(
        "TestServiceImpl.kt",
        """
          package test
          
          import com.google.auto.service.AutoService
          
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
}

package com.fueledbycaffeine.autoservice.functionaltest.fixtures

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.Plugin

// Use the version from gradle.properties since AbstractGradleProject.PLUGIN_UNDER_TEST_VERSION 
// may not be populated correctly in all testkit versions
private const val PLUGIN_VERSION = "0.1.0-SNAPSHOT"

/**
 * Creates test projects for AutoService functional tests.
 */
class AutoServiceProject : AbstractGradleProject() {

  /**
   * Simple single-interface, single-implementation with type inference.
   */
  fun simpleInferred(
    dslKind: GradleProject.DslKind = GradleProject.DslKind.KOTLIN
  ): GradleProject = newGradleProjectBuilder(dslKind)
    .withRootProject {
      withBuildScript {
        plugins(
          Plugin("org.jetbrains.kotlin.jvm", "2.3.0"),
          Plugin("com.fueledbycaffeine.autoservice", PLUGIN_VERSION)
        )
      }
      sources = listOf(
        Source.kotlin(
          """
            package test
            
            interface MyService {
              fun execute()
            }
          """.trimIndent()
        ).withPath("test", "MyService").build(),
        Source.kotlin(
          """
            package test
            
            import com.fueledbycaffeine.autoservice.AutoService
            
            @AutoService
            class MyServiceImpl : MyService {
              override fun execute() = println("MyServiceImpl")
            }
          """.trimIndent()
        ).withPath("test", "MyServiceImpl").build()
      )
    }
    .write()

  /**
   * Single interface with explicit type parameter.
   */
  fun explicitType(
    dslKind: GradleProject.DslKind = GradleProject.DslKind.KOTLIN
  ): GradleProject = newGradleProjectBuilder(dslKind)
    .withRootProject {
      withBuildScript {
        plugins(
          Plugin("org.jetbrains.kotlin.jvm", "2.3.0"),
          Plugin("com.fueledbycaffeine.autoservice", PLUGIN_VERSION)
        )
      }
      sources = listOf(
        Source.kotlin(
          """
            package test
            
            interface Logger {
              fun log(message: String)
            }
          """.trimIndent()
        ).withPath("test", "Logger").build(),
        Source.kotlin(
          """
            package test
            
            import com.fueledbycaffeine.autoservice.AutoService
            
            @AutoService(Logger::class)
            class ConsoleLogger : Logger {
              override fun log(message: String) = println(message)
            }
          """.trimIndent()
        ).withPath("test", "ConsoleLogger").build()
      )
    }
    .write()

  /**
   * Multiple service interfaces on single implementation.
   */
  fun multipleInterfaces(
    dslKind: GradleProject.DslKind = GradleProject.DslKind.KOTLIN
  ): GradleProject = newGradleProjectBuilder(dslKind)
    .withRootProject {
      withBuildScript {
        plugins(
          Plugin("org.jetbrains.kotlin.jvm", "2.3.0"),
          Plugin("com.fueledbycaffeine.autoservice", PLUGIN_VERSION)
        )
      }
      sources = listOf(
        Source.kotlin(
          """
            package test
            
            interface ServiceA { fun doA() }
            interface ServiceB { fun doB() }
          """.trimIndent()
        ).withPath("test", "Services").build(),
        Source.kotlin(
          """
            package test
            
            import com.fueledbycaffeine.autoservice.AutoService
            
            @AutoService(ServiceA::class, ServiceB::class)
            class MultiServiceImpl : ServiceA, ServiceB {
              override fun doA() {}
              override fun doB() {}
            }
          """.trimIndent()
        ).withPath("test", "MultiServiceImpl").build()
      )
    }
    .write()

  /**
   * Multiple implementations of the same service.
   */
  fun multipleImplementations(
    dslKind: GradleProject.DslKind = GradleProject.DslKind.KOTLIN
  ): GradleProject = newGradleProjectBuilder(dslKind)
    .withRootProject {
      withBuildScript {
        plugins(
          Plugin("org.jetbrains.kotlin.jvm", "2.3.0"),
          Plugin("com.fueledbycaffeine.autoservice", PLUGIN_VERSION)
        )
      }
      sources = listOf(
        Source.kotlin(
          """
            package test
            
            interface Logger {
              fun log(message: String)
            }
          """.trimIndent()
        ).withPath("test", "Logger").build(),
        Source.kotlin(
          """
            package test
            
            import com.fueledbycaffeine.autoservice.AutoService
            
            @AutoService
            class ConsoleLogger : Logger {
              override fun log(message: String) = println(message)
            }
          """.trimIndent()
        ).withPath("test", "ConsoleLogger").build(),
        Source.kotlin(
          """
            package test
            
            import com.fueledbycaffeine.autoservice.AutoService
            
            @AutoService
            class FileLogger : Logger {
              override fun log(message: String) {
                // Write to file
              }
            }
          """.trimIndent()
        ).withPath("test", "FileLogger").build()
      )
    }
    .write()

  /**
   * Using Google's annotation.
   */
  fun withGoogleAnnotation(
    dslKind: GradleProject.DslKind = GradleProject.DslKind.KOTLIN
  ): GradleProject = newGradleProjectBuilder(dslKind)
    .withRootProject {
      withBuildScript {
        plugins(
          Plugin("org.jetbrains.kotlin.jvm", "2.3.0"),
          Plugin("com.fueledbycaffeine.autoservice", PLUGIN_VERSION)
        )
        dependencies(
          Dependency("implementation", "com.google.auto.service:auto-service-annotations:1.1.1")
        )
      }
      sources = listOf(
        Source.kotlin(
          """
            package test
            
            interface Logger {
              fun log(message: String)
            }
          """.trimIndent()
        ).withPath("test", "Logger").build(),
        Source.kotlin(
          """
            package test
            
            import com.google.auto.service.AutoService
            
            @AutoService(Logger::class)
            class GoogleLogger : Logger {
              override fun log(message: String) = println(message)
            }
          """.trimIndent()
        ).withPath("test", "GoogleLogger").build()
      )
    }
    .write()

  /**
   * Mixing both our annotation and Google's in the same project.
   */
  fun mixedAnnotations(
    dslKind: GradleProject.DslKind = GradleProject.DslKind.KOTLIN
  ): GradleProject = newGradleProjectBuilder(dslKind)
    .withRootProject {
      withBuildScript {
        plugins(
          Plugin("org.jetbrains.kotlin.jvm", "2.3.0"),
          Plugin("com.fueledbycaffeine.autoservice", PLUGIN_VERSION)
        )
        dependencies(
          Dependency("implementation", "com.google.auto.service:auto-service-annotations:1.1.1")
        )
      }
      sources = listOf(
        Source.kotlin(
          """
            package test
            
            interface Logger {
              fun log(message: String)
            }
          """.trimIndent()
        ).withPath("test", "Logger").build(),
        Source.kotlin(
          """
            package test
            
            import com.fueledbycaffeine.autoservice.AutoService
            
            @AutoService
            class OurLogger : Logger {
              override fun log(message: String) = println("Our: ${"$"}message")
            }
          """.trimIndent()
        ).withPath("test", "OurLogger").build(),
        Source.kotlin(
          """
            package test
            
            import com.google.auto.service.AutoService
            
            @AutoService(Logger::class)
            class GoogleLogger : Logger {
              override fun log(message: String) = println("Google: ${"$"}message")
            }
          """.trimIndent()
        ).withPath("test", "GoogleLogger").build()
      )
    }
    .write()
}

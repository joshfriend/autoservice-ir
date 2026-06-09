package conventions

import com.autonomousapps.GradleTestKitPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class BasePlugin : Plugin<Project> {
  override fun apply(target: Project) = target.run {
    group = providers.gradleProperty("GROUP").get()
    version = providers.gradleProperty("VERSION_NAME").get()

    pluginManager.apply("org.jetbrains.kotlinx.binary-compatibility-validator")
    pluginManager.apply("org.jetbrains.kotlin.jvm")
    pluginManager.apply(PublishConventionPlugin::class.java)
    pluginManager.apply(GradleTestKitPlugin::class.java)

    val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    val jvmTarget = libs.findVersion("jvmTarget").get().toString()
    val jdkLanguage = JavaLanguageVersion.of(libs.findVersion("jdk").get().toString().toInt())

    // Tracks the Kotlin Gradle Plugin version, which determines the compile language version.
    // Several opt-in flags below became the default in 2.4 and are redundant (and fail under
    // allWarningsAsErrors) at that language version, so they are only added for older versions.
    val defaultKotlinVersion = libs.findVersion("kotlin").get().toString()
    val effectiveKotlinVersion = System.getProperty("kotlinVersion") ?: defaultKotlinVersion
    val kotlinVersionParts = effectiveKotlinVersion.split("-")[0].split(".").map { it.toIntOrNull() ?: 0 }
    val isAtLeastKotlin240 =
      kotlinVersionParts[0] > 2 || (kotlinVersionParts[0] == 2 && kotlinVersionParts[1] >= 4)

    // When testing against a non-default Kotlin version, substitute all org.jetbrains.kotlin
    // dependencies so every module (compiler plugin, Gradle plugin, published metadata) compiles
    // and resolves against the same Kotlin API level. This keeps the Kotlin Gradle Plugin pulled
    // into functional-test builds aligned with the version the compiler plugin was built against.
    if (effectiveKotlinVersion != defaultKotlinVersion) {
      configurations.configureEach { configuration ->
        configuration.resolutionStrategy.eachDependency { details ->
          if (details.requested.group == "org.jetbrains.kotlin") {
            details.useVersion(effectiveKotlinVersion)
          }
        }
      }
    }

    extensions.configure(JavaPluginExtension::class.java) { java ->
      java.toolchain.languageVersion.set(jdkLanguage)
    }

    extensions.configure(KotlinProjectExtension::class.java) { kotlin ->
      kotlin.explicitApi()
    }

    tasks.withType(KotlinCompile::class.java).configureEach { task ->
      task.compilerOptions { options ->
        options.allWarningsAsErrors.set(true)
        options.jvmTarget.set(JvmTarget.fromTarget(jvmTarget))
        // Applying annotations to both the value parameter and property is the default as of
        // language version 2.4, where this flag is redundant; only opt in on older versions.
        if (!isAtLeastKotlin240) {
          options.freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
      }
    }

    tasks.withType(JavaCompile::class.java).configureEach { task ->
      task.options.release.set(jvmTarget.toInt())
    }

    tasks.withType(Test::class.java).configureEach { task ->
      task.useJUnit()
      task.testLogging { logging ->
        logging.events("passed", "skipped", "failed", "standardError")
        logging.exceptionFormat = TestExceptionFormat.SHORT
        logging.showExceptions = true
        logging.showCauses = true
      }
    }
  }
}
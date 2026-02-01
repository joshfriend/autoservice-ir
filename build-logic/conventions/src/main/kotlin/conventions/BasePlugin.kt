package conventions

import com.autonomousapps.GradleTestKitPlugin
import com.autonomousapps.GradleTestKitSupportExtension
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

    // Disable automatic test publication creation to prevent conflicts
    // The testkit plugin will still create installForFunctionalTest task
    // and publish existing publications to the functional test repository
    extensions.configure(GradleTestKitSupportExtension::class.java) { ext ->
      ext.disablePublication()
    }

    val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    val jvmTarget = libs.findVersion("jvmTarget").get().toString()
    val jdkLanguage = JavaLanguageVersion.of(libs.findVersion("jdk").get().toString().toInt())

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
        options.freeCompilerArgs.add("-Xannotation-default-target=param-property")
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
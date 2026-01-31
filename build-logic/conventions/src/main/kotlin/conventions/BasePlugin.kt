package conventions

import com.autonomousapps.GradleTestKitPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class BasePlugin : Plugin<Project> {
  override fun apply(target: Project) = target.run {
    pluginManager.apply("org.jetbrains.kotlinx.binary-compatibility-validator")
    pluginManager.apply("org.jetbrains.kotlin.jvm")
    pluginManager.apply(GradleTestKitPlugin::class.java)
    pluginManager.apply(PublishConventionPlugin::class.java)

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
//        options.allWarningsAsErrors.set(true)
        options.jvmTarget.set(JvmTarget.fromTarget(jvmTarget))
      }
    }

    tasks.withType(JavaCompile::class.java).configureEach { task ->
      task.options.release.set(jvmTarget.toInt())
    }

    tasks.withType(Test::class.java).configureEach { task ->
      task.useJUnit()
    }
  }
}
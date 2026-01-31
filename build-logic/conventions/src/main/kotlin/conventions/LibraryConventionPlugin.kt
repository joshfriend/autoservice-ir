package conventions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

class LibraryConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      with(pluginManager) {
        apply("org.jetbrains.kotlinx.binary-compatibility-validator")
        apply("org.jetbrains.kotlin.jvm")
        apply("com.autonomousapps.testkit")
        apply(PublishConventionPlugin::class.java)
      }

      val libs = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
      
      extensions.configure<KotlinProjectExtension> {
        explicitApi()
        compilerOptions {
          jvmTarget.set(JvmTarget.fromTarget(libs.findVersion("jvmTarget").get().toString()))
        }
      }

      extensions.configure<JavaPluginExtension> {
        toolchain {
          languageVersion.set(JavaLanguageVersion.of(libs.findVersion("jdk").get().toString().toInt()))
        }
      }

      tasks.withType<JavaCompile>().configureEach {
        options.release.set(libs.findVersion("jvmTarget").get().toString().toInt())
      }
    }
  }
}

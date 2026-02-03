package conventions

import com.autonomousapps.GradleTestKitSupportExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.plugin.devel.tasks.ValidatePlugins

@Suppress("unused")
class PluginConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) = target.run {
    pluginManager.apply(BasePlugin::class.java)
    pluginManager.apply("java-gradle-plugin")

    // Disable the testKitSupportForJava publication because it conflicts with
    // the pluginMaven publication (same coordinates). The gradle-plugin will still
    // be published to the functional test repository via the pluginMaven publication.
    extensions.configure(GradleTestKitSupportExtension::class.java) { ext ->
      ext.disablePublication()
    }

    tasks.withType(ValidatePlugins::class.java).configureEach { task ->
      task.enableStricterValidation.set(true)
    }

    val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    val defaultKotlinVersion = libs.findVersion("kotlin").get().toString()

    tasks.withType(Test::class.java).configureEach { task ->
      task.systemProperty("gradleVersion", System.getProperty("gradleVersion") ?: "")
      task.systemProperty("kotlinVersion", System.getProperty("kotlinVersion") ?: defaultKotlinVersion)
      task.useJUnitPlatform()
    }
  }
}

package conventions

import com.autonomousapps.GradleTestKitSupportExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.plugin.devel.tasks.ValidatePlugins

@Suppress("unused")
class PluginConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) = target.run {
    pluginManager.apply(BasePlugin::class.java)
    pluginManager.apply("java-gradle-plugin")

    extensions.configure(GradleTestKitSupportExtension::class.java) { testkit ->
      testkit.withSupportLibrary()
      testkit.withTruthLibrary()
    }

    tasks.withType(ValidatePlugins::class.java).configureEach { task ->
      task.enableStricterValidation.set(true)
    }

    tasks.withType(Test::class.java).configureEach { task ->
      task.systemProperty("gradleVersion", System.getProperty("gradleVersion") ?: "")
      task.useJUnitPlatform()
    }
  }
}

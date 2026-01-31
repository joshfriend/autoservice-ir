package conventions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.plugin.devel.tasks.ValidatePlugins

@Suppress("unused")
class PluginConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) = target.run {
    pluginManager.apply(BasePlugin::class.java)
    pluginManager.apply("java-gradle-plugin")

    // Note: We don't call withSupportLibrary() or withTruthLibrary() here
    // because they create the testKitSupportForJava publication which conflicts
    // with the main maven publication. The testkit plugin will still publish
    // artifacts to the functional test repository via installForFunctionalTest task.
    // The support and truth libraries are added as explicit dependencies in
    // the build.gradle.kts file instead.

    tasks.withType(ValidatePlugins::class.java).configureEach { task ->
      task.enableStricterValidation.set(true)
    }

    tasks.withType(Test::class.java).configureEach { task ->
      task.systemProperty("gradleVersion", System.getProperty("gradleVersion") ?: "")
      task.useJUnitPlatform()
    }
  }
}

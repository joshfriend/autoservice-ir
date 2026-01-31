package conventions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

@Suppress("unused")
class LibraryConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) = target.run {
    pluginManager.apply(BasePlugin::class.java)

    tasks.withType(Test::class.java).configureEach { task ->
      task.useJUnit()
    }
  }
}

package conventions

import com.vanniktech.maven.publish.DeploymentValidation
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class PublishConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      pluginManager.apply("com.vanniktech.maven.publish")

      extensions.configure(MavenPublishBaseExtension::class.java) { publishing ->
        publishing.publishToMavenCentral(true, DeploymentValidation.VALIDATED)

        if (providers.gradleProperty("signingInMemoryKey").isPresent) {
          publishing.signAllPublications()
        }

        publishing.pom { pom ->
          pom.description.set("Kotlin compiler plugin implementation of Google's AutoService")
          pom.inceptionYear.set("2026")
          pom.url.set("https://github.com/joshfriend/autoservice-ir/")

          pom.licenses { licensesSpec ->
            licensesSpec.license { license ->
              license.name.set("MIT License")
              license.url.set("https://opensource.org/licenses/MIT")
              license.distribution.set("repo")
            }
          }

          pom.developers { developersSpec ->
            developersSpec.developer { developer ->
              developer.id.set("joshfriend")
              developer.name.set("Josh Friend")
              developer.url.set("https://github.com/joshfriend/")
            }
          }

          pom.scm { scmSpec ->
            scmSpec.url.set("https://github.com/joshfriend/autoservice-ir/")
            scmSpec.connection.set("scm:git:git://github.com/joshfriend/autoservice-ir.git")
            scmSpec.developerConnection.set("scm:git:ssh://git@github.com/joshfriend/autoservice-ir.git")
          }
        }
      }
    }
  }
}

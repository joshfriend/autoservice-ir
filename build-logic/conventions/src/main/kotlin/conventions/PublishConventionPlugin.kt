package conventions

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class PublishConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      pluginManager.apply(com.vanniktech.maven.publish.MavenPublishPlugin::class.java)

      extensions.configure<MavenPublishBaseExtension> {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

        if (providers.gradleProperty("signingInMemoryKey").isPresent) {
          signAllPublications()
        }

        pom {
          description.set(providers.gradleProperty("POM_DESCRIPTION"))
          inceptionYear.set(providers.gradleProperty("POM_INCEPTION_YEAR"))
          url.set(providers.gradleProperty("POM_URL"))

          licenses {
            license {
              name.set(providers.gradleProperty("POM_LICENSE_NAME"))
              url.set(providers.gradleProperty("POM_LICENSE_URL"))
              distribution.set(providers.gradleProperty("POM_LICENSE_DIST"))
            }
          }

          developers {
            developer {
              id.set(providers.gradleProperty("POM_DEVELOPER_ID"))
              name.set(providers.gradleProperty("POM_DEVELOPER_NAME"))
              url.set(providers.gradleProperty("POM_DEVELOPER_URL"))
            }
          }

          scm {
            url.set(providers.gradleProperty("POM_SCM_URL"))
            connection.set(providers.gradleProperty("POM_SCM_CONNECTION"))
            developerConnection.set(providers.gradleProperty("POM_SCM_DEV_CONNECTION"))
          }
        }
      }
    }
  }
}

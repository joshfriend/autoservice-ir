plugins {
  id("org.jetbrains.kotlin.jvm")
  id("java-gradle-plugin")
}

val defaultKotlinVersion = libs.versions.kotlin.get()
val effectiveKotlinVersion: String = System.getProperty("kotlinVersion") ?: defaultKotlinVersion

if (effectiveKotlinVersion != defaultKotlinVersion) {
  configurations.configureEach {
    resolutionStrategy.eachDependency {
      if (requested.group == "org.jetbrains.kotlin") {
        useVersion(effectiveKotlinVersion)
      }
    }
  }
}

kotlin {
  jvmToolchain(21)
}

gradlePlugin {
  plugins {
    register("library") {
      id = "conventions.library"
      implementationClass = "conventions.LibraryConventionPlugin"
    }
    register("plugin") {
      id = "conventions.plugin"
      implementationClass = "conventions.PluginConventionPlugin"
    }
    register("publish") {
      id = "conventions.publish"
      implementationClass = "conventions.PublishConventionPlugin"
    }
  }
}

dependencies {
  implementation(libs.kotlin.gradlePlugin)
  implementation(libs.kotlinx.binaryCompatibilityValidator)
  implementation(libs.vanniktech.mavenPublish)
  implementation(libs.autonomousapps.testkitPlugin)
}

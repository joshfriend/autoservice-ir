plugins {
  id("org.jetbrains.kotlin.jvm")
  id("java-gradle-plugin")
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

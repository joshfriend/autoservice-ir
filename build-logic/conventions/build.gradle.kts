plugins {
  kotlin("jvm")
  `java-gradle-plugin`
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
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
  implementation("org.jetbrains.kotlinx:binary-compatibility-validator:0.17.0")
  implementation("com.vanniktech.maven.publish:com.vanniktech.maven.publish.gradle.plugin:0.36.0")
  implementation("com.autonomousapps:testkit-gradle-plugin:0.17")
}

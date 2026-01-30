plugins {
  alias(libs.plugins.binaryCompatibilityValidator) apply false
  alias(libs.plugins.buildConfig) apply false
  alias(libs.plugins.kotlinJvm) apply false
  alias(libs.plugins.mavenPublish) apply false
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()

dependencyAnalysis {
  reporting {
    printBuildHealth(true)
  }
  issues {
    all {
      onAny {
        severity("fail")
      }
    }
    // gradle-plugin needs :annotations and :compiler-plugin as implementation
    // for testkit to publish them to the functional test repository
    project(":gradle-plugin") {
      onIncorrectConfiguration {
        severity("ignore")
      }
    }
  }
}

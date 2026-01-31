plugins {
  id("org.jetbrains.kotlinx.binary-compatibility-validator") apply false
  id("com.github.gmazzo.buildconfig") apply false
  id("org.jetbrains.kotlin.jvm") apply false
  id("com.vanniktech.maven.publish") apply false
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

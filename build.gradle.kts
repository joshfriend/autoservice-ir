plugins {
  id("org.jetbrains.kotlinx.binary-compatibility-validator") apply false
  id("com.github.gmazzo.buildconfig") apply false
  id("org.jetbrains.kotlin.jvm") apply false
  id("com.vanniktech.maven.publish") apply false
}

val groupName = providers.gradleProperty("GROUP").get()
val versionName = providers.gradleProperty("VERSION_NAME").get()

group = groupName
version = versionName

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

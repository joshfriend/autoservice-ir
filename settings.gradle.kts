@file:Suppress("UnstableApiUsage")

pluginManagement {
  includeBuild("build-logic")
  plugins {
    id("com.gradle.plugin-publish") version "1.3.1"
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("com.autonomousapps.build-health") version "3.5.1"
    id("com.autonomousapps.testkit") version "0.14"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.17.0"
    id("com.github.gmazzo.buildconfig") version "6.0.7"
    id("com.vanniktech.maven.publish") version "0.36.0"
  }
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  id("org.jetbrains.kotlin.jvm") apply false
  id("com.autonomousapps.build-health")
  id("com.autonomousapps.testkit") apply false
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    // R8 releases repository
    maven("https://storage.googleapis.com/r8-releases/raw/") {
      name = "R8-Releases"
      content { includeModule("com.android.tools", "r8") }
    }
  }
}

rootProject.name = "autoservice-ir"

include(":annotations")
include(":compiler-plugin")
include(":gradle-plugin")

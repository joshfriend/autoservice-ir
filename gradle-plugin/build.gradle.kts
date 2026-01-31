plugins {
  id("conventions.plugin")
  id("com.gradle.plugin-publish")
  id("com.github.gmazzo.buildconfig")
}

version = providers.gradleProperty("VERSION_NAME").get()

buildConfig {
  packageName("com.fueledbycaffeine.autoservice.gradle")
  useKotlinOutput {
    topLevelConstants = true
    internalVisibility = true
  }
  buildConfigField("String", "VERSION", providers.gradleProperty("VERSION_NAME").map { "\"$it\"" })
}

gradlePlugin {
  plugins {
    create("autoServicePlugin") {
      id = "com.fueledbycaffeine.autoservice"
      implementationClass = "com.fueledbycaffeine.autoservice.gradle.AutoServiceGradlePlugin"
      displayName = "AutoService Compiler Plugin"
      description = "Kotlin compiler plugin for generating META-INF/services files"
    }
  }
}

mavenPublishing {
  pom {
    name.set("AutoService Gradle Plugin")
  }
}

dependencyAnalysis {
  issues {
    onAny {
      exclude(":annotations")
      exclude(":compiler-plugin")
    }
    onIncorrectConfiguration {
      exclude(libs.autonomousapps.testkit.support)
    }
  }
}


dependencies {
  api(libs.kotlin.gradlePluginApi)
  // These must be implementation for testkit to publish them to functional test repo
  implementation(project(":annotations"))
  implementation(project(":compiler-plugin"))
  implementation(libs.kotlin.stdlib)
  runtimeOnly(libs.kotlin.gradlePlugin)
  
  // Google's AutoService is optional for compatibility
  compileOnly(libs.autoService)

  functionalTestApi(libs.junit5.api)
  functionalTestImplementation(libs.autonomousapps.testkit.support)
  functionalTestImplementation(libs.autonomousapps.testkit.truth)
  functionalTestImplementation(libs.truth)
  functionalTestImplementation(platform(libs.junit5.bom))
  functionalTestImplementation(libs.moshi)
  functionalTestImplementation(libs.moshi.kotlin)
  functionalTestRuntimeOnly(libs.junit5.engine)
  functionalTestRuntimeOnly(libs.junit5.launcher)
  functionalTestCompileOnly(libs.autoService)
}

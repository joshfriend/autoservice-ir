import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  `java-gradle-plugin`
  id("com.gradle.plugin-publish")
  alias(libs.plugins.binaryCompatibilityValidator)
  alias(libs.plugins.buildConfig)
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.mavenPublish)
  id("com.autonomousapps.testkit")
}

kotlin {
  explicitApi()
  compilerOptions {
    jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
  }
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
  publishToMavenCentral(automaticRelease = true)
  if (providers.gradleProperty("signingInMemoryKey").isPresent) {
    signAllPublications()
  }

  pom {
    name.set("AutoService Gradle Plugin")
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

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(libs.versions.jvmTarget.get().toInt())
}

gradleTestKitSupport {
  withSupportLibrary()
  withTruthLibrary()
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

tasks.withType<Test>().configureEach {
  systemProperty("gradleVersion", System.getProperty("gradleVersion") ?: "")
  useJUnitPlatform()
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
  functionalTestImplementation(libs.truth)
  functionalTestImplementation(platform(libs.junit5.bom))
  functionalTestRuntimeOnly(libs.junit5.engine)
  functionalTestRuntimeOnly(libs.junit5.launcher)
  functionalTestCompileOnly(libs.autoService)
}

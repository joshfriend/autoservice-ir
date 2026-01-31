import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.binaryCompatibilityValidator)
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.mavenPublish)
  id("com.autonomousapps.testkit")
}

kotlin {
  explicitApi()
  compilerOptions {
    jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    freeCompilerArgs.addAll(
      "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
      "-Xcontext-parameters"
    )
  }
}

mavenPublishing {
  publishToMavenCentral(automaticRelease = true)
  if (providers.gradleProperty("signingInMemoryKey").isPresent) {
    signAllPublications()
  }

  pom {
    name.set("AutoService Compiler Plugin")
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

dependencies {
  implementation(libs.kotlin.stdlib)
  compileOnly(project(":annotations"))
  compileOnly(libs.kotlin.compilerEmbeddable)
  
  // Google's AutoService is optional - we support both
  compileOnly(libs.autoService)

  // These are needed at test runtime for inheritClassPath in kotlin-compile-testing
  // Split into compileOnly + runtimeOnly so DAGP doesn't complain
  testCompileOnly(project(":annotations"))
  testCompileOnly(libs.autoService)
  testRuntimeOnly(project(":annotations"))
  testRuntimeOnly(libs.autoService)
  testImplementation(libs.kotlin.reflect)
  testRuntimeOnly(libs.kotlin.compilerEmbeddable)
  testImplementation(libs.kotlinCompileTesting)
  testImplementation(libs.junit4)
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnit()
}


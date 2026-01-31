import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("org.jetbrains.kotlinx.binary-compatibility-validator")
  id("org.jetbrains.kotlin.jvm")
  id("com.vanniktech.maven.publish")
  id("com.autonomousapps.testkit")
}

kotlin {
  explicitApi()
  compilerOptions {
    jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
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

mavenPublishing {
  publishToMavenCentral(automaticRelease = true)
  if (providers.gradleProperty("signingInMemoryKey").isPresent) {
    signAllPublications()
  }

  pom {
    name.set("AutoService Annotations")
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


plugins {
  id("conventions.library")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll(
      "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
      "-Xcontext-parameters"
    )
  }
}

mavenPublishing {
  pom {
    name.set("AutoService Compiler Plugin")
  }
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
  testRuntimeOnly(libs.kotlin.metadata.jvm)
  testImplementation(libs.kotlinCompileTesting)
  testImplementation(libs.junit4)
  testImplementation(kotlin("test"))
}

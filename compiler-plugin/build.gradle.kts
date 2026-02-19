plugins {
  id("conventions.library")
  `java-test-fixtures`
  idea
}

val kotlinVersionParts = libs.versions.kotlin.get().split("-")[0].split(".").map { it.toInt() }
val isKotlin2320OrLater = kotlinVersionParts[0] > 2 ||
  (kotlinVersionParts[0] == 2 && kotlinVersionParts[1] > 3) ||
  (kotlinVersionParts[0] == 2 && kotlinVersionParts[1] == 3 && kotlinVersionParts[2] >= 20)

sourceSets {
  test {
    // java.srcDir is configured below via generateTests task output
    kotlin.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(listOf("testData"))
  }
  named("testFixtures") {
    val versionSpecificDir = if (isKotlin2320OrLater) "kotlin-2.3.20" else "kotlin-pre-2.3.20"
    kotlin.srcDir("src/testFixtures/$versionSpecificDir")
  }
}

idea { module.generatedSourceDirs.add(projectDir.resolve("test-gen/java")) }

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll(
      "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
      "-Xcontext-parameters"
    )
  }
}

// Configuration for AutoService runtime annotations during compilation tests
val autoServiceRuntime by configurations.dependencyScope("autoServiceRuntime") { isTransitive = false }

val autoServiceRuntimeClasspath =
  configurations.resolvable("autoServiceRuntimeClasspath") {
    isTransitive = false
    extendsFrom(autoServiceRuntime)
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

  // testFixtures dependencies for the Kotlin compiler test framework
  testFixturesRuntimeOnly(libs.kotlin.testJunit5)
  testFixturesImplementation(libs.opentest4j)
  testFixturesApi(libs.kotlin.compilerTestFramework)
  testFixturesApi(libs.kotlin.compiler)

  testImplementation(libs.junit5.api)

  // Runtime annotations for tests
  autoServiceRuntime(project(":annotations"))
  autoServiceRuntime(libs.autoService)

  // Dependencies required to run the internal test framework
  testRuntimeOnly(libs.junit4)
  testRuntimeOnly(libs.kotlin.reflect)
  testRuntimeOnly(libs.kotlin.test)
  testRuntimeOnly(libs.kotlin.scriptRuntime)
  testRuntimeOnly(libs.kotlin.annotationsJvm)
}

tasks.test {
  dependsOn(autoServiceRuntimeClasspath)
  val autoServiceRuntimeClasspath = autoServiceRuntimeClasspath.map { it.asPath }

  // Re-run tests when testData files change
  inputs
    .dir(layout.projectDirectory.dir("testData"))
    .withPropertyName("testData")
    .withPathSensitivity(PathSensitivity.RELATIVE)

  useJUnitPlatform()
  workingDir = rootDir

  // Show exception messages (including diffs) in test output
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    showExceptions = true
    showCauses = false
    showStackTraces = false
  }

  systemProperty("autoservice.jvmTarget", libs.versions.jvmTarget.get())

  doFirst { systemProperty("autoServiceRuntime.classpath", autoServiceRuntimeClasspath.get()) }

  // Properties required to run the internal test framework
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib", "kotlin-stdlib")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", "kotlin-stdlib-jdk8")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-reflect", "kotlin-reflect")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-test", "kotlin-test")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-script-runtime", "kotlin-script-runtime")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-annotations-jvm", "kotlin-annotations-jvm")

  systemProperty("idea.ignore.disabled.plugins", "true")
  systemProperty("idea.home.path", rootDir)
}

val generateTests =
  tasks.register<JavaExec>("generateTests") {
    inputs
      .dir(layout.projectDirectory.dir("testData"))
      .withPropertyName("testData")
      .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(layout.projectDirectory.dir("test-gen")).withPropertyName("generatedTests")

    classpath = sourceSets.testFixtures.get().runtimeClasspath
    mainClass.set("com.fueledbycaffeine.autoservice.compiler.GenerateTestsKt")
    workingDir = rootDir
  }

tasks.compileTestKotlin { dependsOn(generateTests) }

// Wire the generated test sources to the test source set so Gradle understands the dependency
sourceSets.test {
  java.srcDir(generateTests.map { it.outputs.files.singleFile })
}

fun Test.setLibraryProperty(propName: String, jarName: String) {
  val path =
    project.configurations
      .testRuntimeClasspath
      .get()
      .files
      .find { """org.jetbrains.kotlin\W$jarName\W""".toRegex().containsMatchIn(it.path) }
      ?.absolutePath
      ?: return
  systemProperty(propName, path)
}

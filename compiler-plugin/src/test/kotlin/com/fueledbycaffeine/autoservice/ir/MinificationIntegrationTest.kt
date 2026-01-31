package com.fueledbycaffeine.autoservice.ir

import com.android.tools.r8.CompilationMode
import com.android.tools.r8.OutputMode
import com.android.tools.r8.R8
import com.android.tools.r8.R8Command
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import proguard.Configuration
import proguard.ConfigurationParser
import proguard.ProGuard
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.ServiceLoader
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests that verify AutoService's annotation-based ProGuard rules work correctly
 * with both ProGuard and R8 minification.
 *
 * These tests:
 * 1. Compile Kotlin code with AutoService annotations
 * 2. Package the output into a JAR
 * 3. Run ProGuard or R8 to minify the JAR using the annotation-based rules
 * 4. Verify that ServiceLoader can still discover the service implementations
 * 
 * The ProGuard rules are bundled in the annotations artifact at:
 * META-INF/proguard/autoservice-annotations.pro
 * 
 * R8 is run with --classfile flag to produce JVM class files instead of DEX.
 */
@RunWith(Parameterized::class)
class MinificationIntegrationTest(
  private val minifierName: String,
  private val minifierFactory: (TemporaryFolder) -> Minifier
) {

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun minifiers(): List<Array<Any>> = listOf(
      arrayOf("ProGuard", { tempFolder: TemporaryFolder -> ProGuardMinifier(tempFolder) }),
      arrayOf("R8", { tempFolder: TemporaryFolder -> R8Minifier(tempFolder) })
    )
  }

  @get:Rule
  val tempFolder = TemporaryFolder()

  private val minifier: Minifier by lazy { minifierFactory(tempFolder) }

  @Test
  fun `preserves service implementations with annotation-based rules`() {
    val result = compileWithAutoService()
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

    val inputJar = createJarFromCompilation(result, "input.jar")

    val outputJar = File(tempFolder.root, "output.jar")
    minifier.minify(inputJar, outputJar, proguardRules, keepServiceInterfaces = true)

    verifyServicesInJar(outputJar, "test.MyService", listOf("test.MyServiceImpl"))
  }

  @Test
  fun `preserves nested class service implementations`() {
    val result = compileNestedClassService()
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

    val inputJar = createJarFromCompilation(result, "input-nested.jar")

    val outputJar = File(tempFolder.root, "output-nested.jar")
    minifier.minify(inputJar, outputJar, proguardRules, keepServiceInterfaces = true)

    verifyServicesInJar(outputJar, "test.MyService", listOf("test.Outer\$NestedServiceImpl"))
  }

  @Test
  fun `preserves multiple service implementations`() {
    val result = compileMultipleServices()
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

    val inputJar = createJarFromCompilation(result, "input-multi.jar")

    val outputJar = File(tempFolder.root, "output-multi.jar")
    minifier.minify(inputJar, outputJar, proguardRules, keepServiceInterfaces = true)

    verifyServicesInJar(outputJar, "test.MyService", listOf("test.MyServiceImpl1", "test.MyServiceImpl2"))
  }

  @Test
  fun `annotation-based rules format is valid proguard syntax`() {
    val content = proguardRules.readText()
    
    // Verify basic ProGuard rule structure for annotation-based rules
    assertTrue(content.contains("# AutoService annotation-based ProGuard/R8 rules"))
    assertTrue(content.contains("-keep @com.google.auto.service.AutoService class * {"))
    assertTrue(content.contains("<init>();"))
    assertTrue(content.contains("}"))
    
    // Verify the rules can be parsed without errors
    val configuration = Configuration()
    val tempRulesFile = File(tempFolder.root, "test-parse-rules.pro")
    tempRulesFile.writeText("""
      # Minimal config to test parsing
      -dontwarn **
      -dontoptimize
      $content
    """.trimIndent())
    
    ConfigurationParser(tempRulesFile, System.getProperties()).use { parser ->
      parser.parse(configuration)
    }
  }

  // ========================= Compilation Helpers =========================

  private fun compileWithAutoService(): JvmCompilationResult {
    return compile(
      SourceFile.kotlin(
        "MyService.kt",
        """
          package test
          
          interface MyService {
            fun doSomething(): String
          }
        """
      ),
      SourceFile.kotlin(
        "MyServiceImpl.kt",
        """
          package test
          
          import com.google.auto.service.AutoService
          
          @AutoService(MyService::class)
          class MyServiceImpl : MyService {
            override fun doSomething(): String = "Hello from MyServiceImpl"
          }
        """
      )
    )
  }

  private fun compileNestedClassService(): JvmCompilationResult {
    return compile(
      SourceFile.kotlin(
        "Outer.kt",
        """
          package test
          
          import com.google.auto.service.AutoService
          
          interface MyService {
            fun doSomething(): String
          }
          
          class Outer {
            @AutoService(MyService::class)
            class NestedServiceImpl : MyService {
              override fun doSomething(): String = "Hello from nested"
            }
          }
        """
      )
    )
  }

  private fun compileMultipleServices(): JvmCompilationResult {
    return compile(
      SourceFile.kotlin(
        "MyService.kt",
        """
          package test
          
          interface MyService {
            fun doSomething(): String
          }
        """
      ),
      SourceFile.kotlin(
        "MyServiceImpl1.kt",
        """
          package test
          
          import com.google.auto.service.AutoService
          
          @AutoService(MyService::class)
          class MyServiceImpl1 : MyService {
            override fun doSomething(): String = "Hello from Impl1"
          }
        """
      ),
      SourceFile.kotlin(
        "MyServiceImpl2.kt",
        """
          package test
          
          import com.google.auto.service.AutoService
          
          @AutoService(MyService::class)
          class MyServiceImpl2 : MyService {
            override fun doSomething(): String = "Hello from Impl2"
          }
        """
      )
    )
  }

  private fun compile(
    vararg sourceFiles: SourceFile
  ): JvmCompilationResult {
    return KotlinCompilation().apply {
      sources = sourceFiles.asList()
      compilerPluginRegistrars = listOf(AutoServiceComponentRegistrar())
      commandLineProcessors = listOf(AutoServiceCommandLineProcessor())
      System.setProperty("kotlin.output.dir", workingDir.absolutePath)
      inheritClassPath = true
      messageOutputStream = System.out
      verbose = false
    }.compile().also {
      System.clearProperty("kotlin.output.dir")
    }
  }

  private fun createJarFromCompilation(result: JvmCompilationResult, jarName: String): File {
    val outputJar = File(tempFolder.root, jarName)
    JarOutputStream(outputJar.outputStream()).use { jos ->
      result.outputDirectory.walkTopDown()
        .filter { it.isFile }
        .forEach { file ->
          val entryName = file.relativeTo(result.outputDirectory).path.replace(File.separator, "/")
          jos.putNextEntry(JarEntry(entryName))
          file.inputStream().use { it.copyTo(jos) }
          jos.closeEntry()
        }
    }
    return outputJar
  }

  /**
   * Loads the annotation-based ProGuard rules from the annotations artifact resources.
   * These rules use annotation matching to keep all @AutoService-annotated classes.
   */
  private val proguardRules: File by lazy {
    val resourceStream = this::class.java.getResourceAsStream("/META-INF/proguard/autoservice-annotations.pro")
      ?: error("Could not find annotation-based ProGuard rules in resources")
    
    val rulesFile = File(tempFolder.root, "autoservice-annotations.pro")
    resourceStream.use { input ->
      rulesFile.outputStream().use { output ->
        input.copyTo(output)
      }
    }
    rulesFile
  }

  // ========================= JAR Verification =========================

  private fun verifyServicesInJar(
    jarFile: File,
    serviceInterface: String,
    expectedImplementations: List<String>
  ) {
    assertTrue(jarFile.exists(), "Output JAR should exist: ${jarFile.absolutePath}")

    JarFile(jarFile).use { jar ->
      // Check service file exists
      val serviceEntry = jar.getJarEntry("META-INF/services/$serviceInterface")
      assertNotNull(serviceEntry, "Service file for $serviceInterface should exist in JAR")

      // Read and verify service file contents
      val serviceContent = jar.getInputStream(serviceEntry).bufferedReader().readText()
      val registeredImpls = serviceContent.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }

      for (impl in expectedImplementations) {
        assertTrue(
          registeredImpls.contains(impl),
          "Service file should contain $impl, but found: $registeredImpls"
        )
      }

      // Verify implementation classes exist in JAR
      for (impl in expectedImplementations) {
        val classPath = impl.replace('.', '/') + ".class"
        val classEntry = jar.getJarEntry(classPath)
        assertNotNull(classEntry, "Implementation class $impl should exist in JAR at $classPath")
      }
    }

    // Verify ServiceLoader can actually load the services
    URLClassLoader(arrayOf(jarFile.toURI().toURL()), this::class.java.classLoader).use { classLoader ->
      val serviceClass = Class.forName(serviceInterface, true, classLoader)
      val services = ServiceLoader.load(serviceClass, classLoader).toList()
      
      assertTrue(
        services.isNotEmpty(),
        "ServiceLoader should find at least one implementation for $serviceInterface"
      )
      assertEquals(
        expectedImplementations.size,
        services.size,
        "ServiceLoader should find exactly ${expectedImplementations.size} implementations"
      )
    }
  }
}

// ========================= Minifier Abstraction =========================

/**
 * Abstraction for code minifiers (ProGuard, R8) to allow testing with both.
 */
interface Minifier {
  fun minify(
    inputJar: File,
    outputJar: File,
    proguardRules: File?,
    keepServiceInterfaces: Boolean = false
  )
}

/**
 * ProGuard minifier implementation.
 */
class ProGuardMinifier(private val tempFolder: TemporaryFolder) : Minifier {
  
  override fun minify(
    inputJar: File,
    outputJar: File,
    proguardRules: File?,
    keepServiceInterfaces: Boolean
  ) {
    val configuration = Configuration()
    
    val rules = buildString {
      appendLine("-injars ${inputJar.absolutePath}")
      appendLine("-outjars ${outputJar.absolutePath}")
      
      // Library JARs (Java runtime)
      val javaHome = System.getProperty("java.home")
      val jmodPath = File(javaHome, "jmods/java.base.jmod")
      if (jmodPath.exists()) {
        appendLine("-libraryjars ${jmodPath.absolutePath}(!**.jar;!module-info.class)")
      } else {
        val rtJar = File(javaHome, "lib/rt.jar")
        if (rtJar.exists()) {
          appendLine("-libraryjars ${rtJar.absolutePath}")
        }
      }
      
      // Kotlin stdlib
      findKotlinStdlib()?.let { appendLine("-libraryjars $it") }
      
      // Basic settings
      appendLine("-dontoptimize")
      appendLine("-dontwarn **")
      appendLine("-dontobfuscate")
      appendLine("-keepdirectories META-INF/services")
      appendLine("-adaptresourcefilecontents META-INF/services/*")
      
      if (keepServiceInterfaces) {
        appendLine("-keep interface test.MyService { *; }")
      }
      
      if (proguardRules != null) {
        appendLine("-include ${proguardRules.absolutePath}")
      }
    }
    
    val rulesFile = File(tempFolder.root, "proguard-rules-${System.nanoTime()}.pro")
    rulesFile.writeText(rules)
    
    ConfigurationParser(rulesFile, System.getProperties()).use { parser ->
      parser.parse(configuration)
    }
    
    ProGuard(configuration).execute()
  }
}

/**
 * R8 minifier implementation (uses --classfile for JVM output).
 */
class R8Minifier(private val tempFolder: TemporaryFolder) : Minifier {
  
  override fun minify(
    inputJar: File,
    outputJar: File,
    proguardRules: File?,
    keepServiceInterfaces: Boolean
  ) {
    val javaHome = System.getProperty("java.home")
    
    val rules = buildString {
      appendLine("-dontoptimize")
      appendLine("-dontwarn **")
      appendLine("-dontobfuscate")
      appendLine("-keepattributes *")
      
      if (keepServiceInterfaces) {
        appendLine("-keep interface test.MyService { *; }")
      }
      
      if (proguardRules != null) {
        appendLine()
        append(proguardRules.readText())
      }
    }
    
    val rulesFile = File(tempFolder.root, "r8-rules-${System.nanoTime()}.pro")
    rulesFile.writeText(rules)
    
    val outputDir = Files.createTempDirectory(tempFolder.root.toPath(), "r8-output").toFile()
    
    val commandBuilder = R8Command.builder()
      .addProgramFiles(inputJar.toPath())
      .addProguardConfigurationFiles(rulesFile.toPath())
      .setOutput(outputDir.toPath(), OutputMode.ClassFile)
      .setMode(CompilationMode.RELEASE)
    
    // Add Java runtime library
    val rtJar = File(javaHome, "lib/rt.jar")
    if (rtJar.exists()) {
      commandBuilder.addLibraryFiles(rtJar.toPath())
    } else {
      val jmodsDir = File(javaHome, "jmods")
      if (jmodsDir.exists()) {
        jmodsDir.listFiles()?.filter { it.name.endsWith(".jmod") }?.forEach {
          commandBuilder.addLibraryFiles(it.toPath())
        }
      }
    }
    
    // Kotlin stdlib
    findKotlinStdlib()?.let { commandBuilder.addLibraryFiles(File(it).toPath()) }
    
    R8.run(commandBuilder.build())
    
    // Package R8 output into JAR
    val r8Files = outputDir.walkTopDown().filter { it.isFile }.toList()
    JarOutputStream(outputJar.outputStream()).use { jos ->
      r8Files.forEach { file ->
        val entryName = file.relativeTo(outputDir).path.replace(File.separator, "/")
        jos.putNextEntry(JarEntry(entryName))
        file.inputStream().use { it.copyTo(jos) }
        jos.closeEntry()
      }
    }
  }
}

/**
 * Finds the Kotlin stdlib JAR on the classpath.
 */
private fun findKotlinStdlib(): String? {
  return System.getProperty("java.class.path")
    .split(File.pathSeparator)
    .find { it.contains("kotlin-stdlib") && !it.contains("kotlin-stdlib-common") && it.endsWith(".jar") }
}

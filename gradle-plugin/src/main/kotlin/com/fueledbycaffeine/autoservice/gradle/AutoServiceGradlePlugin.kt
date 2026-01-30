package com.fueledbycaffeine.autoservice.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

public class AutoServiceGradlePlugin : KotlinCompilerPluginSupportPlugin {
  
  override fun apply(target: Project) {
    target.extensions.create("autoService", AutoServiceExtension::class.java)
    
    // Automatically add our annotations to the compile classpath
    // Users don't need to manually add this dependency
    target.dependencies.add("implementation", "com.fueledbycaffeine.autoservice:annotations:$VERSION")
  }

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun getCompilerPluginId(): String = "com.fueledbycaffeine.autoservice.compiler"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = "com.fueledbycaffeine.autoservice",
    artifactId = "compiler-plugin",
    version = VERSION,
  )

  override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>
  ): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.getByType(AutoServiceExtension::class.java)

    return project.provider {
      val options = mutableListOf<SubpluginOption>()
      
      if (extension.debug.isPresent) {
        options.add(SubpluginOption("debug", extension.debug.get().toString()))
      }
      
      // Set output directory to the compilation's destination directory
      val outputDir = kotlinCompilation.defaultSourceSet.kotlin.classesDirectory.get().asFile
      options.add(SubpluginOption("outputDir", outputDir.absolutePath))
      
      // Pass project root for relative path display in error messages
      options.add(SubpluginOption("projectRoot", project.rootDir.absolutePath))
      
      options
    }
  }
}

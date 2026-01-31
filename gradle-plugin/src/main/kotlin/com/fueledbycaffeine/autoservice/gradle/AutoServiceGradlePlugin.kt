package com.fueledbycaffeine.autoservice.gradle

import com.fueledbycaffeine.autoservice.ir.AutoServiceCommandLineProcessor
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

public class AutoServiceGradlePlugin : KotlinCompilerPluginSupportPlugin {
  
  override fun apply(target: Project) {
    AutoServiceExtension.create(target.extensions)
  }

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun getCompilerPluginId(): String = AutoServiceCommandLineProcessor.PLUGIN_ID

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = GROUP,
    artifactId = "compiler-plugin",
    version = VERSION,
  )

  override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>
  ): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.getByType(AutoServiceExtension::class.java)

    // Automatically add our annotations to the compile classpath
    // Users don't need to manually add this dependency
    project.dependencies.add(
      kotlinCompilation.defaultSourceSet.implementationConfigurationName,
      "$GROUP:annotations:$VERSION",
    )

    return project.provider {
      buildList {
        add(SubpluginOption(
          AutoServiceCommandLineProcessor.OPTION_NAME_DEBUG,
          extension.debug.get().toString(),
        ))

        // Set output directory to the compilation's destination directory
        add(SubpluginOption(
          AutoServiceCommandLineProcessor.OPTION_NAME_OUTPUT_DIR,
          kotlinCompilation.defaultSourceSet.kotlin.classesDirectory.get().asFile.absolutePath,
        ))

        // Pass project root for relative path display in error messages
        add(SubpluginOption(
          AutoServiceCommandLineProcessor.OPTION_NAME_PROJECT_ROOT,
          project.rootDir.absolutePath,
        ))
      }
    }
  }
}

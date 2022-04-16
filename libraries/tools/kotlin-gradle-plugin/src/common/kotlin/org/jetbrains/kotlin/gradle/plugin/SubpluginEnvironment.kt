package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

class SubpluginEnvironment(
    private val subplugins: List<KotlinCompilerPluginSupportPlugin>,
    private val kotlinPluginVersion: String
) {
    companion object {
        fun loadSubplugins(project: Project): SubpluginEnvironment {
            val kotlinPluginVersion = project.getKotlinPluginVersion()
            return SubpluginEnvironment(project.plugins.filterIsInstance<KotlinCompilerPluginSupportPlugin>(), kotlinPluginVersion)
        }
    }

    fun addSubpluginOptions(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>
    ): List<KotlinCompilerPluginSupportPlugin> {
        val appliedSubplugins = subplugins.filter { it.isApplicable(kotlinCompilation) }
        for (subplugin in appliedSubplugins) {
            if (!subplugin.isApplicable(kotlinCompilation)) continue

            val pluginId = subplugin.getCompilerPluginId()
            project.logger.kotlinDebug { "Loading subplugin $pluginId" }


            if (kotlinCompilation is AbstractKotlinNativeCompilation && !kotlinCompilation.useGenericPluginArtifact) {
                subplugin.getPluginArtifactForNative()?.let { artifact ->
                    project.addMavenDependency(kotlinCompilation.pluginConfigurationName, artifact)
                }
            } else {
                project.addMavenDependency(kotlinCompilation.pluginConfigurationName, subplugin.getPluginArtifact())
            }

            val subpluginOptionsProvider = subplugin.applyToCompilation(kotlinCompilation)
            val subpluginId = subplugin.getCompilerPluginId()

            val configureKotlinTask: (KotlinCompile<*>) -> Unit = {
                val pluginOptions = it.getPluginOptions()
                val subpluginOptions = subpluginOptionsProvider.get()
                for (option in subpluginOptions) {
                    pluginOptions.addPluginArgument(subpluginId, option)
                }
                it.registerSubpluginOptionsAsInputs(subpluginId, subpluginOptions)
            }

            kotlinCompilation.compileKotlinTaskProvider.configure(configureKotlinTask)

            if (kotlinCompilation is KotlinJsIrCompilation) {
                kotlinCompilation.binaries.all {
                    if (it is JsIrBinary) {
                        it.linkTask.configure(configureKotlinTask)
                    }
                }
            }

            project.logger.kotlinDebug("Subplugin $pluginId loaded")
        }

        return appliedSubplugins
    }

    private fun KotlinCompile<*>.getPluginOptions(): CompilerPluginOptions = when (this) {
        is AbstractKotlinCompile<*> -> pluginOptions
        is KotlinNativeCompile -> compilerPluginOptions
        else -> error("Unexpected task ${this.name}, class: ${this.javaClass}")
    }

    private fun Project.addMavenDependency(configuration: String, artifact: SubpluginArtifact) {
        val artifactVersion = artifact.version ?: kotlinPluginVersion
        val mavenCoordinate = "${artifact.groupId}:${artifact.artifactId}:$artifactVersion"
        project.logger.kotlinDebug { "Adding '$mavenCoordinate' to '$configuration' configuration" }
        project.dependencies.add(configuration, mavenCoordinate)
    }
}

internal fun addCompilationSourcesToExternalCompileTask(
    compilation: KotlinCompilation<*>,
    task: TaskProvider<out AbstractKotlinCompileTool<*>>
) {
    if (compilation is KotlinJvmAndroidCompilation) {
        compilation.androidVariant.forEachKotlinSourceSet { sourceSet ->
            task.configure { it.setSource(sourceSet.kotlin) }
        }
        compilation.androidVariant.forEachJavaSourceDir { sources ->
            task.configure { it.setSource(sources.dir) }
        }
    } else {
        task.configure { taskInstance ->
            compilation.allKotlinSourceSets.forEach { sourceSet -> taskInstance.setSource(sourceSet.kotlin) }
        }
    }
}


internal fun findJavaTaskForKotlinCompilation(compilation: KotlinCompilation<*>): TaskProvider<out JavaCompile>? =
    when (compilation) {
        is KotlinJvmAndroidCompilation -> compilation.compileJavaTaskProvider
        is KotlinWithJavaCompilation -> compilation.compileJavaTaskProvider
        is KotlinJvmCompilation -> compilation.compileJavaTaskProvider // may be null for Kotlin-only JVM target in MPP
        else -> null
    }

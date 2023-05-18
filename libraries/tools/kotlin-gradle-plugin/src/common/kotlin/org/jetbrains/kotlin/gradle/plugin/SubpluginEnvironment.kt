package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.internal.kaptGenerateStubsTaskName
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.whenKaptEnabled

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
                    project.addMavenDependency(kotlinCompilation.internal.configurations.pluginConfiguration.name, artifact)
                }
            } else {
                project.addMavenDependency(
                    kotlinCompilation.internal.configurations.pluginConfiguration.name, subplugin.getPluginArtifact()
                )
            }

            val subpluginOptionsProvider = subplugin.applyToCompilation(kotlinCompilation)
            val subpluginId = subplugin.getCompilerPluginId()
            val compilerOptions = subpluginOptionsProvider.map { subpluginOptions ->
                val options = CompilerPluginOptions()
                subpluginOptions.forEach { opt ->
                    options.addPluginArgument(subpluginId, opt)
                }
                options
            }

            val configureKotlinTask: (KotlinCompilationTask<*>) -> Unit = {
                when (it) {
                    is AbstractKotlinCompile<*> -> it.pluginOptions.add(compilerOptions)
                    is KotlinNativeCompile -> it.compilerPluginOptions.addPluginArgument(compilerOptions.get())
                    else -> error("Unexpected task ${it.name}, class: ${it.javaClass}")
                }
            }

            kotlinCompilation.compileTaskProvider.configure(configureKotlinTask)
            project.configurePluginOptionsForKapt(kotlinCompilation, configureKotlinTask)

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

    private fun Project.configurePluginOptionsForKapt(
        kotlinCompilation: KotlinCompilation<*>,
        configureKotlinTask: (KotlinCompilationTask<*>) -> Unit,
    ) {
        if (kotlinCompilation is KotlinJvmCompilation ||
            kotlinCompilation is KotlinWithJavaCompilation<*, *> ||
            kotlinCompilation is KotlinJvmAndroidCompilation
        ) {
            whenKaptEnabled {
                @Suppress("UNCHECKED_CAST")
                val kaptGenerateStubsTaskName = (kotlinCompilation.compileTaskProvider as TaskProvider<KotlinJvmCompile>)
                    .kaptGenerateStubsTaskName
                tasks.withType<KaptGenerateStubs>().configureEach { task ->
                    if (task.name == kaptGenerateStubsTaskName) {
                        configureKotlinTask(task)
                    }
                }
            }
        }
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
        compilation.androidVariant.forEachKotlinSourceDirectorySet(compilation.project) { sourceSet ->
            task.configure { it.setSource(sourceSet) }
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
        is KotlinWithJavaCompilation<*, *> -> compilation.compileJavaTaskProvider
        is KotlinJvmCompilation -> compilation.compileJavaTaskProvider // may be null for Kotlin-only JVM target in MPP
        else -> null
    }

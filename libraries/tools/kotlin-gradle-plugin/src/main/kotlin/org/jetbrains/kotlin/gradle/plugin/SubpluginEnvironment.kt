package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.thisTaskProvider
import java.util.*

class SubpluginEnvironment(
    private val subplugins: List<KotlinCompilerPluginSupportPlugin>,
    private val kotlinPluginVersion: String
) {
    companion object {
        fun loadSubplugins(project: Project, kotlinPluginVersion: String): SubpluginEnvironment =
            try {
                @Suppress("DEPRECATION") // support for the deprecated plugin API
                val klass = KotlinGradleSubplugin::class.java
                val buildscriptClassloader = project.buildscript.classLoader
                val klassFromBuildscript = try {
                    buildscriptClassloader.loadClass(klass.canonicalName)
                } catch (e: ClassNotFoundException) {
                    null
                }

                val classloader = if (klass == klassFromBuildscript) {
                    buildscriptClassloader
                } else {
                    klass.classLoader
                }

                val result = project.plugins.filterIsInstance<KotlinCompilerPluginSupportPlugin>()

                @Suppress("DEPRECATION", "UNCHECKED_CAST")
                val compatibilitySubplugins = ServiceLoader.load(klass, classloader)
                    .filter { it !is KotlinCompilerPluginSupportPlugin }
                    .map { LegacyKotlinCompilerPluginSupportPlugin(it as KotlinGradleSubplugin<AbstractCompile>) }

                SubpluginEnvironment(result + compatibilitySubplugins, kotlinPluginVersion)
            } catch (e: NoClassDefFoundError) {
                // Skip plugin loading if KotlinGradleSubplugin is not defined.
                // It is true now for tests in kotlin-gradle-plugin-core.
                project.logger.error("Could not load subplugins", e)
                SubpluginEnvironment(listOf(), kotlinPluginVersion)
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

            subplugin.getPluginArtifact().let { artifact ->
                project.addMavenDependency(PLUGIN_CLASSPATH_CONFIGURATION_NAME, artifact)
            }

            subplugin.getPluginArtifactForNative()?.let { artifact ->
                project.addMavenDependency(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, artifact)
            }

            val subpluginOptionsProvider = subplugin.applyToCompilation(kotlinCompilation)
            val subpluginId = subplugin.getCompilerPluginId()

            kotlinCompilation.compileKotlinTaskProvider.configure {
                val pluginOptions = it.getPluginOptions()
                val subpluginOptions = subpluginOptionsProvider.get()
                for (option in subpluginOptions) {
                    pluginOptions.addPluginArgument(subpluginId, option)
                }
                it.registerSubpluginOptionsAsInputs(subpluginId, subpluginOptions)
            }

            project.logger.kotlinDebug("Subplugin $pluginId loaded")

            if (subplugin is LegacyKotlinCompilerPluginSupportPlugin) {
                subplugin.getPluginKotlinTasks(kotlinCompilation).forEach { task ->
                    addCompilationSourcesToExternalCompileTask(kotlinCompilation, task.thisTaskProvider)
                }
            }
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

internal fun addCompilationSourcesToExternalCompileTask(compilation: KotlinCompilation<*>, task: TaskProvider<out AbstractCompile>) {
    if (compilation is KotlinJvmAndroidCompilation) {
        compilation.androidVariant.forEachKotlinSourceSet { sourceSet -> task.configure { it.source(sourceSet.kotlin) } }
        compilation.androidVariant.forEachJavaSourceDir { sources -> task.configure { it.source(sources.dir) } }
    } else {
        task.configure { taskInstance ->
            compilation.allKotlinSourceSets.forEach { sourceSet -> taskInstance.source(sourceSet.kotlin) }
        }
    }
}

internal class LegacyKotlinCompilerPluginSupportPlugin(
    @Suppress("deprecation") // support for deprecated API
    val oldPlugin: KotlinGradleSubplugin<AbstractCompile>
): KotlinCompilerPluginSupportPlugin {
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        oldPlugin.isApplicable(kotlinCompilation.target.project, kotlinCompilation.compileKotlinTaskProvider.get() as AbstractCompile)

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val androidProjectHandlerOrNull: AbstractAndroidProjectHandler? = if (kotlinCompilation is KotlinJvmAndroidCompilation)
            KotlinAndroidPlugin.androidTargetHandler(
                checkNotNull(project.getKotlinPluginVersion()),
                kotlinCompilation.target as KotlinAndroidTarget
            ) else null

        val variantData = (kotlinCompilation as? KotlinJvmAndroidCompilation)?.androidVariant

        val result = oldPlugin.apply(
            project,
            kotlinCompilation.compileKotlinTask as AbstractCompile,
            findJavaTaskForKotlinCompilation(kotlinCompilation)?.get(),
            variantData,
            androidProjectHandlerOrNull,
            if (variantData != null) null else kotlinCompilation
        )

        return project.provider { result }
    }

    fun getPluginKotlinTasks(compilation: KotlinCompilation<*>): List<AbstractCompile> {
        val project = compilation.target.project
        return oldPlugin.getSubpluginKotlinTasks(project, compilation.compileKotlinTask as AbstractCompile)
    }

    override fun getCompilerPluginId(): String = oldPlugin.getCompilerPluginId()
    override fun getPluginArtifact(): SubpluginArtifact = oldPlugin.getPluginArtifact()
    override fun getPluginArtifactForNative(): SubpluginArtifact? = oldPlugin.getNativeCompilerPluginArtifact()
}

internal fun findJavaTaskForKotlinCompilation(compilation: KotlinCompilation<*>): TaskProvider<out JavaCompile>? =
    when (compilation) {
        is KotlinJvmAndroidCompilation -> compilation.compileJavaTaskProvider
        is KotlinWithJavaCompilation -> compilation.compileJavaTaskProvider
        is KotlinJvmCompilation -> compilation.compileJavaTaskProvider // may be null for Kotlin-only JVM target in MPP
        else -> null
    }

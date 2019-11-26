package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import java.util.*

class SubpluginEnvironment(
    private val subplugins: List<KotlinCompilerPluginSupportPlugin>,
    private val kotlinPluginVersion: String
) {
    companion object {
        @Suppress("DEPRECATION")
        fun loadSubplugins(project: Project, kotlinPluginVersion: String): SubpluginEnvironment =
            try {
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

                val compatibilitySubplugins = ServiceLoader.load(KotlinGradleSubplugin::class.java, classloader)
                    .map { oldPluginAsLifecycleAwarePlugin(@Suppress("UNCHECKED_CAST") (it as KotlinGradleSubplugin<AbstractCompile>)) }

                val lifecycleAwareSubplugins = project.plugins.filterIsInstance<KotlinCompilerPluginSupportPlugin>()

                SubpluginEnvironment(compatibilitySubplugins + lifecycleAwareSubplugins, kotlinPluginVersion)
            } catch (e: NoClassDefFoundError) {
                // Skip plugin loading if KotlinGradleSubplugin is not defined.
                // It is true now for tests in kotlin-gradle-plugin-core.
                project.logger.error("Could not load subplugins", e)
                SubpluginEnvironment(listOf(), kotlinPluginVersion)
            }
    }

    fun addSubpluginOptions(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        javaTask: TaskProvider<out AbstractCompile>? = null
    ): List<KotlinCompilerPluginSupportPlugin> {
        val appliedSubplugins = subplugins.filter { it.isApplicable(kotlinCompilation) }
        for (subplugin in appliedSubplugins) {
            if (!subplugin.isApplicable(kotlinCompilation)) continue

            val pluginId = subplugin.getCompilerPluginId()
            project.logger.kotlinDebug { "Loading subplugin $pluginId" }

            subplugin.getPluginArtifact().let { artifact ->
                project.addMavenDependency(PLUGIN_CLASSPATH_CONFIGURATION_NAME, artifact)
            }

            subplugin.getNativeCompilerPluginArtifact()?.let { artifact ->
                project.addMavenDependency(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, artifact)
            }

            val subpluginOptionsProvider = subplugin.applyToCompilation(kotlinCompilation, javaTask)
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

private fun <T : AbstractCompile> oldPluginAsLifecycleAwarePlugin(
    @Suppress("DEPRECATION")
    oldPlugin: KotlinGradleSubplugin<T>
): KotlinCompilerPluginSupportPlugin = object : KotlinCompilerPluginSupportPlugin {
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        oldPlugin.isApplicable(kotlinCompilation.target.project, kotlinCompilation.compileKotlinTaskProvider.get() as AbstractCompile)

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
        javaCompile: TaskProvider<out AbstractCompile>?
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val androidProjectHandlerOrNull: AbstractAndroidProjectHandler? = if (kotlinCompilation is KotlinJvmAndroidCompilation)
            KotlinAndroidPlugin.androidTargetHandler(
                checkNotNull(project.getKotlinPluginVersion()),
                kotlinCompilation.target as KotlinAndroidTarget
            ) else null

        val variantData = (kotlinCompilation as? KotlinJvmAndroidCompilation)?.androidVariant

        @Suppress("UNCHECKED_CAST")
        val result = oldPlugin.apply(
            project,
            kotlinCompilation.compileKotlinTask as T,
            javaCompile?.get(),
            variantData,
            androidProjectHandlerOrNull,
            if (variantData != null) null else kotlinCompilation
        )

        return project.provider { result }
    }

    override fun getPluginKotlinTasks(compilation: KotlinCompilation<*>): List<TaskProvider<out AbstractCompile>> {
        val project = compilation.target.project

        @Suppress("UNCHECKED_CAST")
        val tasks = oldPlugin.getSubpluginKotlinTasks(project, compilation.compileKotlinTask as T)
        val allAbstractCompileTasks = project.tasks.withType(AbstractCompile::class.java)
        return tasks.map { allAbstractCompileTasks.named(it.name) }
    }

    override fun getCompilerPluginId(): String = oldPlugin.getCompilerPluginId()
    override fun getPluginArtifact(): SubpluginArtifact = oldPlugin.getPluginArtifact()
    override fun getNativeCompilerPluginArtifact(): SubpluginArtifact? = oldPlugin.getNativeCompilerPluginArtifact()
}
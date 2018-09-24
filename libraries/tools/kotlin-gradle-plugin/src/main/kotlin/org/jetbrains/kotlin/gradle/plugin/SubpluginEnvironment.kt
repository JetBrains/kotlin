package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import java.util.*

class SubpluginEnvironment(
    private val subplugins: List<KotlinGradleSubplugin<AbstractCompile>>,
    private val kotlinPluginVersion: String
) {
    companion object {
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

                val subplugins = ServiceLoader.load(KotlinGradleSubplugin::class.java, classloader)
                    .map { @Suppress("UNCHECKED_CAST") (it as KotlinGradleSubplugin<AbstractCompile>) }

                SubpluginEnvironment(subplugins, kotlinPluginVersion)
            } catch (e: NoClassDefFoundError) {
                // Skip plugin loading if KotlinGradleSubplugin is not defined.
                // It is true now for tests in kotlin-gradle-plugin-core.
                project.logger.error("Could not load subplugins", e)
                SubpluginEnvironment(listOf(), kotlinPluginVersion)
            }
    }

    fun <C : CommonCompilerArguments> addSubpluginOptions(
        project: Project,
        kotlinTask: AbstractKotlinCompile<C>,
        javaTask: AbstractCompile? = null,
        variantData: Any? = null,
        androidProjectHandler: AbstractAndroidProjectHandler<out Any?>? = null,
        kotlinCompilation: KotlinCompilation? = null
    ): List<KotlinGradleSubplugin<AbstractKotlinCompile<C>>> = addSubpluginOptions(
        project,
        kotlinTask,
        kotlinTask.pluginOptions,
        javaTask,
        variantData,
        androidProjectHandler,
        kotlinCompilation
    )

    fun <C : CommonCompilerArguments> addSubpluginOptions(
        project: Project,
        kotlinTask: AbstractCompile,
        pluginOptions: CompilerPluginOptions,
        javaTask: AbstractCompile? = null,
        variantData: Any? = null,
        androidProjectHandler: AbstractAndroidProjectHandler<out Any?>? = null,
        kotlinCompilation: KotlinCompilation? = null
    ): List<KotlinGradleSubplugin<AbstractKotlinCompile<C>>> {
        val appliedSubplugins = subplugins.filter { it.isApplicable(project, kotlinTask) }
        for (subplugin in appliedSubplugins) {
            if (!subplugin.isApplicable(project, kotlinTask)) continue

            val pluginId = subplugin.getCompilerPluginId()
            project.logger.kotlinDebug { "Loading subplugin $pluginId" }

            val artifact = subplugin.getPluginArtifact()
            val artifactVersion = artifact.version ?: kotlinPluginVersion
            val mavenCoordinate = "${artifact.groupId}:${artifact.artifactId}:$artifactVersion"
            project.logger.kotlinDebug { "Adding '$mavenCoordinate' to '$PLUGIN_CLASSPATH_CONFIGURATION_NAME' configuration" }
            project.dependencies.add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, mavenCoordinate)

            val subpluginOptions =
                subplugin.apply(project, kotlinTask, javaTask, variantData, androidProjectHandler, kotlinCompilation)
            val subpluginId = subplugin.getCompilerPluginId()
            kotlinTask.registerSubpluginOptionsAsInputs(subpluginId, subpluginOptions)

            for (option in subpluginOptions) {
                pluginOptions.addPluginArgument(subpluginId, option)
            }
            project.logger.kotlinDebug("Subplugin $pluginId loaded")
        }

        return appliedSubplugins
    }
}
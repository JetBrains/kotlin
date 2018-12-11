/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.gradle.scripting.internal

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.transform.ArtifactTransform
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.scripting.ScriptingExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.useLazyTaskConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptDefinitionsFromClasspathDiscoverySource
import java.io.File

private const val MISCONFIGURATION_MESSAGE_SUFFIX = "the plugin is probably applied by a mistake"

class ScriptingGradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(ScriptingGradleSubplugin::class.java) != null

        fun configureForSourceSet(project: Project, sourceSetName: String) {

            if (!org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast(4, 0)) return

            val discoveryConfiguration = project.configurations.maybeCreate(getDiscoveryClasspathConfigurationName(sourceSetName)).apply {
                isVisible = false
                isCanBeConsumed = false
                description = "Script filename extensions discovery classpath configuration"
            }

            configureDiscoveryTransformation(project, discoveryConfiguration, getDiscoveryResultsConfigurationName(sourceSetName))
        }
    }

    override fun apply(project: Project) {

        if (!org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast(4, 0)) return

        project.afterEvaluate {

            val javaPluginConvention = project.convention.findPlugin(JavaPluginConvention::class.java)
            if (javaPluginConvention?.sourceSets?.isEmpty() == false) {

                val configureAction: (KotlinCompile) -> (Unit) = { task ->

                    if (task !is KaptGenerateStubsTask) {

                        val discoveryClasspathConfigurationName = getDiscoveryClasspathConfigurationName(task.sourceSetName)
                        project.configurations.findByName(discoveryClasspathConfigurationName)?.let { _ ->
                            configureScriptsExtensions(project, javaPluginConvention, task.sourceSetName)
                        }
                            ?: project.logger.warn("kotlin scripting plugin: $project.${task.name} - configuration not found: $discoveryClasspathConfigurationName, $MISCONFIGURATION_MESSAGE_SUFFIX")
                    }
                }
                if (useLazyTaskConfiguration) {
                    project.tasks.withType(KotlinCompile::class.java).configureEach(configureAction)
                } else {
                    project.tasks.withType(KotlinCompile::class.java, configureAction)
                }
            } else {
                project.logger.warn("kotlin scripting plugin: applied to a non-JVM project $project, $MISCONFIGURATION_MESSAGE_SUFFIX")
            }
        }
    }

    private fun configureScriptsExtensions(
        project: Project,
        javaPluginConvention: JavaPluginConvention,
        sourceSetName: String
    ) {
        javaPluginConvention.sourceSets.findByName(sourceSetName)?.let { sourceSet ->

            val discoveryResultsConfigurationName = getDiscoveryResultsConfigurationName(sourceSetName)

            val kotlinSourceSet = sourceSet.getConvention(KOTLIN_DSL_NAME) as? KotlinSourceSet
            if (kotlinSourceSet == null) {
                project.logger.warn("kotlin scripting plugin: kotlin source set not found: $project.$sourceSet, $MISCONFIGURATION_MESSAGE_SUFFIX")
            } else {
                val extensions by lazy {
                    val discoveryResultsConfiguration = project.configurations.findByName(discoveryResultsConfigurationName)
                    if (discoveryResultsConfiguration == null) {
                        project.logger.warn("kotlin scripting plugin: discovery results not found: $project.$discoveryResultsConfigurationName, $MISCONFIGURATION_MESSAGE_SUFFIX")
                        emptySet<String>()
                    } else {
                        discoveryResultsConfiguration.files.flatMapTo(HashSet()) {
                            it.readLines().filter(String::isNotBlank)
                        }.also {
                            kotlinSourceSet.addCustomSourceFilesExtensions(it.toList())
                            project.logger.debug("kotlin scripting plugin: $project.$sourceSet: discovered script extensions: $it")
                        }
                    }
                }
                kotlinSourceSet.kotlin.filter.include { it.file.extension in extensions }
            }
        }
    }

}

private const val MAIN_CONFIGURATION_NAME = "kotlinScriptDef"
private const val RESULTS_CONFIGURATION_SUFFIX = "Extensions"

private fun getDiscoveryClasspathConfigurationName(sourceSetName: String): String = when (sourceSetName) {
    "main" -> MAIN_CONFIGURATION_NAME
    else -> "$sourceSetName${MAIN_CONFIGURATION_NAME.capitalize()}"
}

private fun getDiscoveryResultsConfigurationName(sourceSetName: String): String =
    getDiscoveryClasspathConfigurationName(sourceSetName) + RESULTS_CONFIGURATION_SUFFIX



private fun configureDiscoveryTransformation(
    project: Project,
    discoveryConfiguration: Configuration,
    discoveryResultsConfigurationName: String
) {
    project.configurations.maybeCreate(discoveryResultsConfigurationName).apply {
        isCanBeConsumed = false
    }
    project.dependencies.apply {
        add(
            discoveryResultsConfigurationName,
            project.withRegisteredDiscoverScriptExtensionsTransform {
                discoveryConfiguration.discoverScriptExtensionsFiles()
            }
        )
    }
}

internal class DiscoverScriptExtensionsTransform : ArtifactTransform() {

    override fun transform(input: File): List<File> {
        val definitions =
            ScriptDefinitionsFromClasspathDiscoverySource(
                listOf(input), emptyMap(),
                PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false)
            ).definitions
        val extensions = definitions.mapTo(arrayListOf()) { it.fileExtension }
        return if (extensions.isNotEmpty()) {
            val outputFile = outputDirectory.resolve("${input.nameWithoutExtension}.discoveredScriptsExtensions.txt")
            outputFile.writeText(extensions.joinToString("\n"))
            listOf(outputFile)
        } else emptyList()
    }
}

private
fun Project.registerDiscoverScriptExtensionsTransform() {
    dependencies.apply {
        registerTransform {
            with(it) {
                from.attribute(artifactType, "jar")
                to.attribute(artifactType, scriptFilesExtensions)
                artifactTransform(DiscoverScriptExtensionsTransform::class.java)
            }
        }
        registerTransform {
            with(it) {
                from.attribute(artifactType, "classes")
                to.attribute(artifactType, scriptFilesExtensions)
                artifactTransform(DiscoverScriptExtensionsTransform::class.java)
            }
        }
    }
}

private
fun <T> Project.withRegisteredDiscoverScriptExtensionsTransform(block: () -> T): T {
    if (!project.extensions.extraProperties.has("DiscoverScriptExtensionsTransform")) {
        registerDiscoverScriptExtensionsTransform()
        project.extensions.extraProperties["DiscoverScriptExtensionsTransform"] = true
    }
    return block()
}

private val artifactType = Attribute.of("artifactType", String::class.java)

private val scriptFilesExtensions = "script-files-extensions"

private
fun Configuration.discoverScriptExtensionsFiles() =
    incoming.artifactView {
        attributes {
            it.attribute(artifactType, scriptFilesExtensions)
        }
    }.artifacts.artifactFiles


class ScriptingKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        const val SCRIPTING_ARTIFACT_NAME = "kotlin-scripting-compiler-embeddable"

        val SCRIPT_DEFINITIONS_OPTION = "script-definitions"
        val SCRIPT_DEFINITIONS_CLASSPATH_OPTION = "script-definitions-classpath"
        val DISABLE_SCRIPT_DEFINITIONS_FROM_CLSSPATH_OPTION = "disable-script-definitions-from-classpath"
        val LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION = "script-resolver-environment"
    }

    override fun isApplicable(project: Project, task: AbstractCompile) =
        ScriptingGradleSubplugin.isEnabled(project)

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<*>?
    ): List<SubpluginOption> {
        if (!ScriptingGradleSubplugin.isEnabled(project)) return emptyList()

        val scriptingExtension = project.extensions.findByType(ScriptingExtension::class.java)
            ?: project.extensions.create("kotlinScripting", ScriptingExtension::class.java)

        val options = mutableListOf<SubpluginOption>()

        for (scriptDef in scriptingExtension.myScriptDefinitions) {
            options += SubpluginOption(SCRIPT_DEFINITIONS_OPTION, scriptDef)
        }
        for (path in scriptingExtension.myScriptDefinitionsClasspath) {
            options += SubpluginOption(SCRIPT_DEFINITIONS_CLASSPATH_OPTION, path)
        }
        if (scriptingExtension.myDisableScriptDefinitionsFromClasspath) {
            options += SubpluginOption(DISABLE_SCRIPT_DEFINITIONS_FROM_CLSSPATH_OPTION, "true")
        }
        for (pair in scriptingExtension.myScriptResolverEnvironment) {
            options += SubpluginOption(LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION, "${pair.key}=${pair.value}")
        }

        return options
    }

    override fun getCompilerPluginId() = "kotlin.scripting"
    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(artifactId = SCRIPTING_ARTIFACT_NAME)
}

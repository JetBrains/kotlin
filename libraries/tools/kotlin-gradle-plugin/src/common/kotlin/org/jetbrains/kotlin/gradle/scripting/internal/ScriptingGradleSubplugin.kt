/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.gradle.scripting.internal

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.internal.JavaSourceSetsAccessor
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.scripting.ScriptingExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.reporter
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsFromClasspathDiscoverySource
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

private const val SCRIPTING_LOG_PREFIX = "kotlin scripting plugin:"

class ScriptingGradleSubplugin : Plugin<Project> {
    companion object {
        const val MISCONFIGURATION_MESSAGE_SUFFIX = "the plugin is probably applied by a mistake"

        fun configureForSourceSet(project: Project, sourceSetName: String) {
            val discoveryConfiguration = project.configurations.maybeCreate(getDiscoveryClasspathConfigurationName(sourceSetName)).apply {
                isVisible = false
                isCanBeConsumed = false
                description = "Script filename extensions discovery classpath configuration"
            }
            project.logger.info("$SCRIPTING_LOG_PREFIX created the scripting discovery configuration: ${discoveryConfiguration.name}")

            configureDiscoveryTransformation(project, discoveryConfiguration, getDiscoveryResultsConfigurationName(sourceSetName))
        }
    }

    override fun apply(project: Project) {
        project.plugins.apply(ScriptingKotlinGradleSubplugin::class.java)

        project.afterEvaluate {
            val javaSourceSets = project
                .variantImplementationFactory<JavaSourceSetsAccessor.JavaSourceSetsAccessorVariantFactory>()
                .getInstance(project)
                .sourceSetsIfAvailable
            if (javaSourceSets?.isEmpty() == false) {

                val configureAction: (KotlinCompile) -> (Unit) = ca@{ task ->

                    if (task !is KaptGenerateStubsTask) {
                        val sourceSetName = task.sourceSetName.orNull ?: return@ca
                        val discoveryResultsConfigurationName = getDiscoveryResultsConfigurationName(sourceSetName)
                        val discoveryResultConfiguration = project.configurations.findByName(discoveryResultsConfigurationName)
                        if (discoveryResultConfiguration == null) {
                            project.logger.warn(
                                "$SCRIPTING_LOG_PREFIX $project.${task.name} - configuration not found:" +
                                        " $discoveryResultsConfigurationName, $MISCONFIGURATION_MESSAGE_SUFFIX"
                            )
                        } else {
                            task.scriptDefinitions.from(discoveryResultConfiguration)
                        }
                    }
                }

                project.tasks.withType(KotlinCompile::class.java).configureEach(configureAction)
            } else {
                // TODO: implement support for discovery in MPP project: use KotlinSourceSet directly and do not rely on java convevtion sourcesets
                if (project.multiplatformExtensionOrNull == null) {
                    project.logger.warn("$SCRIPTING_LOG_PREFIX applied to a non-JVM project $project, $MISCONFIGURATION_MESSAGE_SUFFIX")
                }
            }
        }
    }
}

private const val MAIN_CONFIGURATION_NAME = "kotlinScriptDef"
private const val RESULTS_CONFIGURATION_SUFFIX = "Extensions"

private fun getDiscoveryClasspathConfigurationName(sourceSetName: String): String = when (sourceSetName) {
    "main" -> MAIN_CONFIGURATION_NAME
    else -> "$sourceSetName${MAIN_CONFIGURATION_NAME.capitalizeAsciiOnly()}"
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
        isCanBeResolved = true
        attributes.attribute(artifactType, scriptFilesExtensions)
        extendsFrom(discoveryConfiguration)
    }
    project.dependencies.registerOnceDiscoverScriptExtensionsTransform()
}

@CacheableTransform
internal abstract class DiscoverScriptExtensionsTransformAction : TransformAction<TransformParameters.None> {
    @get:Classpath
    @get:InputArtifact
    @get:NormalizeLineEndings
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile

        val definitions =
            ScriptDefinitionsFromClasspathDiscoverySource(
                listOf(input),
                defaultJvmScriptingHostConfiguration,
                PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false).reporter
            ).definitions

        val extensions = definitions.mapTo(arrayListOf()) { it.fileExtension }

        if (extensions.isNotEmpty()) {
            val outputFile = outputs.file("${input.nameWithoutExtension}.discoveredScriptsExtensions.txt")
            outputFile.writeText(extensions.joinToString("\n"))
        }
    }
}

private fun DependencyHandler.registerDiscoverScriptExtensionsTransform() {
    registerTransform(DiscoverScriptExtensionsTransformAction::class.java) { transformSpec ->
        transformSpec.from.attribute(artifactType, "jar")
        transformSpec.to.attribute(artifactType, scriptFilesExtensions)
    }

    registerTransform(DiscoverScriptExtensionsTransformAction::class.java) { transformSpec ->
        transformSpec.from.attribute(artifactType, "classes")
        transformSpec.to.attribute(artifactType, scriptFilesExtensions)
    }
}

private fun DependencyHandler.registerOnceDiscoverScriptExtensionsTransform() {
    if (!extensions.extraProperties.has("DiscoverScriptExtensionsTransform")) {
        registerDiscoverScriptExtensionsTransform()
        extensions.extraProperties["DiscoverScriptExtensionsTransform"] = true
    }
}

private val artifactType = Attribute.of("artifactType", String::class.java)

private const val scriptFilesExtensions = "script-files-extensions"

private fun Configuration.discoverScriptExtensionsFiles() =
    incoming.artifactView {
        attributes {
            it.attribute(artifactType, scriptFilesExtensions)
        }
    }.artifacts.artifactFiles


class ScriptingKotlinGradleSubplugin : KotlinCompilerPluginSupportPlugin {
    companion object {
        const val SCRIPTING_ARTIFACT_NAME = "kotlin-scripting-compiler-embeddable"

        val SCRIPT_DEFINITIONS_OPTION = "script-definitions"
        val SCRIPT_DEFINITIONS_CLASSPATH_OPTION = "script-definitions-classpath"
        val DISABLE_SCRIPT_DEFINITIONS_FROM_CLSSPATH_OPTION = "disable-script-definitions-from-classpath"
        val LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION = "script-resolver-environment"
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = kotlinCompilation !is AbstractKotlinNativeCompilation

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val scriptingExtension = project.extensions.findByType(ScriptingExtension::class.java)
            ?: project.extensions.create("kotlinScripting", ScriptingExtension::class.java)

        return project.provider {
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

            options
        }
    }

    override fun getCompilerPluginId() = "kotlin.scripting"
    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(artifactId = SCRIPTING_ARTIFACT_NAME)
}

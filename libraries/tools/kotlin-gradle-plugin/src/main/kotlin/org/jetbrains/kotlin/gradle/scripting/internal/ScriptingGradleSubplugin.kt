/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.scripting.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.scripting.ScriptingExtension
import org.jetbrains.kotlin.gradle.tasks.GradleMessageCollector
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptDefinitionsFromClasspathDiscoverySource
import kotlin.properties.Delegates

class ScriptingGradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(ScriptingGradleSubplugin::class.java) != null

        val MAIN_CONFIGURATION_NAME = "kotlinScriptDef"

        fun getConfigurationName(sourceSetName: String): String = when (sourceSetName) {
            "main" -> MAIN_CONFIGURATION_NAME
            else -> "$sourceSetName${MAIN_CONFIGURATION_NAME.capitalize()}"
        }

        fun createDiscoveryConfigurationIfNeeded(project: Project, sourceSetName: String) {
            project.configurations.maybeCreate(getConfigurationName(sourceSetName))
        }
    }

    override fun apply(project: Project) {

        project.afterEvaluate {

            val javaPluginConvention = project.convention.findPlugin(JavaPluginConvention::class.java)
            if (javaPluginConvention?.sourceSets?.isEmpty() == false) {

                project.tasks.all { task ->
                    if (task is KotlinCompile && task !is KaptGenerateStubsTask) {
                        val configuration = project.configurations.findByName(getConfigurationName(task.sourceSetName))
                        if (configuration?.isEmpty == false) {
                            javaPluginConvention.sourceSets.findByName(task.sourceSetName)?.let { sourceSet ->
                                val extensionsTask =
                                    project.tasks.create(
                                        "discover${task.sourceSetName.capitalize()}ScriptsExtensions",
                                        DiscoverScriptExtensionsTask::class.java
                                    )
                                extensionsTask.sourceSet = sourceSet
                                extensionsTask.discoveryClasspathConfiguration = configuration
                                extensionsTask.kotlinCompile = task
                                task.dependsOn.add(extensionsTask)
                            }
                        }
                    }
                }
            } else {
                project.logger.warn("kotlin scripting plugin: applied to a non-JVM project $project")
            }
        }
    }
}

open class DiscoverScriptExtensionsTask : DefaultTask() {

    @get:Internal
    internal var sourceSet: SourceSet by Delegates.notNull()

    @get:InputFiles
    @get:Classpath
    internal var discoveryClasspathConfiguration: Configuration by Delegates.notNull()

    @get:Internal
    internal var kotlinCompile: KotlinCompile by Delegates.notNull()

    @Input
    override fun getDependsOn(): MutableSet<Any> = kotlinCompile.dependsOn

    @TaskAction
    @Suppress("unused")
    fun findKnownScriptExtensions() {
        val scriptingClasspath = discoveryClasspathConfiguration.files.takeIf { it.isNotEmpty() } ?: return

        val definitions =
            ScriptDefinitionsFromClasspathDiscoverySource(
                scriptingClasspath.toList(), emptyMap(),
                GradleMessageCollector(project.logger)
            ).definitions
        val extensions = definitions.mapTo(arrayListOf(), KotlinScriptDefinition::fileExtension)
        val kotlinSourceSet = sourceSet.getConvention(KOTLIN_DSL_NAME) as? KotlinSourceSet
        if (kotlinSourceSet == null) {
            project.logger.warn("kotlin scripting plugin: kotlin source set not found: $project.$sourceSet")
        } else if (extensions.isNotEmpty()) {
            project.logger.info("kotlin scripting plugin: Add new extensions to the sourceset $project.$sourceSet: $extensions")
            kotlinSourceSet.kotlin.filter.include(extensions.map { "**/*.$it" })
        }
    }
}

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
        kotlinCompilation: KotlinCompilation?
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

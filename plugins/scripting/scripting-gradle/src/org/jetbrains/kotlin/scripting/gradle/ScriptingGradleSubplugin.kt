/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.JetBrainsSubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class ScriptingGradleSubplugin : Plugin<Project>
{
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(ScriptingGradleSubplugin::class.java) != null
    }

    override fun apply(project: Project) {}
}

class ScriptingKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        const val SCRIPTING_ARTIFACT_NAME = "kotlin-scripting"

        val SCRIPT_DEFINITIONS_OPTION = "script-definitions"
        val SCRIPT_DEFINITIONS_CLASSPATH_OPTION = "script-definitions-classpath"
        val DISABLE_SCRIPT_DEFINITIONS_FROM_CLSSPATH_OPTION = "disable-script-definitions-from-classpath"
        val LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION = "script-resolver-environment"
    }

    override fun isApplicable(project: Project, task: AbstractCompile) = ScriptingGradleSubplugin.isEnabled(project)

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile,
        variantData: Any?,
        androidProjectHandler: Any?,
        javaSourceSet: SourceSet?
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

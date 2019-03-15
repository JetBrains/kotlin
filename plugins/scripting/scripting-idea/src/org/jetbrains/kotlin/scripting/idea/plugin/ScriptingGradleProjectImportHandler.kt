/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.idea.plugin

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.idea.configuration.GradleProjectImportHandler
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCommandLineProcessor
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import java.io.File

class ScriptingGradleProjectImportHandler : GradleProjectImportHandler {

    val compilerPluginId = ScriptingCommandLineProcessor.PLUGIN_ID
    val gradlePluginJars = listOf(
        "scripting-gradle", // obsolete artifact name, only for compatibility with 1.2.5x, where it was introduced (and immediately dropped afterwards)
        "scripting-compiler",
        "scripting-compiler-embeddable"
    )

    override fun importBySourceSet(
        facet: KotlinFacet,
        sourceSetNode: com.intellij.openapi.externalSystem.model.DataNode<GradleSourceSetData>
    ) {
        modifyCompilerArgumentsForPlugin(facet, compilerPluginId, gradlePluginJars)
    }

    override fun importByModule(
        facet: KotlinFacet,
        moduleNode: com.intellij.openapi.externalSystem.model.DataNode<com.intellij.openapi.externalSystem.model.project.ModuleData>
    ) {
        modifyCompilerArgumentsForPlugin(facet, compilerPluginId, gradlePluginJars)
    }
}

// NOTE: partially copied from idePluginUtil.kt, it is not possible to reuse it right now without refactoring
internal fun modifyCompilerArgumentsForPlugin(
    facet: KotlinFacet,
    compilerPluginId: String,
    pluginJarNames: List<String>
) {
    val facetSettings = facet.configuration.settings

    // investigate why copyBean() sometimes throws exceptions
    val commonArguments = facetSettings.compilerArguments ?: CommonCompilerArguments.DummyImpl()

    // TODO: find out where new options should come from (or maybe they are not needed here at all)
//    val newOptionsForPlugin = setup?.options?.map { "plugin:$compilerPluginId:${it.key}=${it.value}" } ?: emptyList()

    val oldAllPluginOptions = (commonArguments.pluginOptions ?: emptyArray()).filterTo(mutableListOf()) { !it.startsWith("plugin:$compilerPluginId:") }
    val newAllPluginOptions = oldAllPluginOptions // + newOptionsForPlugin

    val filterRegexes = pluginJarNames.map {
        "(kotlin-)?(maven-)?$it-.*\\.jar".toRegex()
    }
    val oldPluginClasspaths = (commonArguments.pluginClasspaths ?: emptyArray()).filterTo(mutableListOf()) {
        val lastIndexOfFile = it.lastIndexOfAny(charArrayOf('/', File.separatorChar))
        if (lastIndexOfFile < 0) {
            return@filterTo true
        }
        !it.drop(lastIndexOfFile + 1).let { fileName ->
            filterRegexes.any { fileName.matches(it) }
        }
    }

    // TODO: find out how to make it - see comment to the newOptionsForPlugin above
    val newPluginClasspaths = oldPluginClasspaths // + (setup?.classpath ?: emptyList())

    commonArguments.pluginOptions = newAllPluginOptions.toTypedArray()
    commonArguments.pluginClasspaths = newPluginClasspaths.toTypedArray()

    facetSettings.compilerArguments = commonArguments
}

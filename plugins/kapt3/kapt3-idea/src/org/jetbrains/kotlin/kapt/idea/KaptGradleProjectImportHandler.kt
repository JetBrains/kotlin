/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.idea

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.idea.configuration.GradleProjectImportHandler
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import java.io.File

class KaptGradleProjectImportHandler : GradleProjectImportHandler {
    override fun importBySourceSet(facet: KotlinFacet, sourceSetNode: DataNode<GradleSourceSetData>) {
        modifyCompilerArgumentsForPlugin(facet)
    }

    override fun importByModule(facet: KotlinFacet, moduleNode: DataNode<ModuleData>) {
        modifyCompilerArgumentsForPlugin(facet)
    }

    private fun modifyCompilerArgumentsForPlugin(facet: KotlinFacet) {
        val facetSettings = facet.configuration.settings

        // Can't reuse const in Kapt3CommandLineProcessor, we don't have Kapt in the IDEA plugin
        val compilerPluginId = "org.jetbrains.kotlin.kapt3"
        val compilerArguments = facetSettings.compilerArguments ?: CommonCompilerArguments.DummyImpl()

        val newPluginOptions = (compilerArguments.pluginOptions ?: emptyArray()).filter { !it.startsWith("plugin:$compilerPluginId:") }
        val newPluginClasspath = (compilerArguments.pluginClasspaths ?: emptyArray()).filter { !isKaptCompilerPluginPath(it) }

        fun List<String>.toArrayIfNotEmpty() = takeIf { it.isNotEmpty() }?.toTypedArray()

        compilerArguments.pluginOptions = newPluginOptions.toArrayIfNotEmpty()
        compilerArguments.pluginClasspaths = newPluginClasspath.toArrayIfNotEmpty()

        facetSettings.compilerArguments = compilerArguments
    }

    private fun isKaptCompilerPluginPath(path: String): Boolean {
        val lastIndexOfFile = path.lastIndexOfAny(charArrayOf('/', File.separatorChar)).takeIf { it >= 0 } ?: return false
        val fileName = path.drop(lastIndexOfFile + 1).toLowerCase()
        return fileName.matches("kotlin\\-annotation\\-processing(\\-gradle)?\\-[0-9].*\\.jar".toRegex())
    }
}
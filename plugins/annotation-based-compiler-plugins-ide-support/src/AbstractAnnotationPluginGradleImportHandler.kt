/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.annotation.plugin.ide

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.plugin.ide.CompilerPluginSetup.PluginOption
import org.jetbrains.kotlin.idea.configuration.GradleProjectImportHandler
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.plugin.ide.CompilerPluginSetup
import org.jetbrains.kotlin.plugin.ide.modifyCompilerArgumentsForPlugin
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import java.io.File

abstract class AbstractAnnotationPluginGradleImportHandler<T : AnnotationBasedPluginModel> : GradleProjectImportHandler {
    abstract val compilerPluginId: String
    abstract val pluginName: String
    abstract val annotationOptionName: String
    abstract val pluginJarFileFromIdea: File

    abstract val modelKey: Key<T>

    override fun importBySourceSet(facet: KotlinFacet, sourceSetNode: DataNode<GradleSourceSetData>) {
        modifyCompilerArgumentsForPlugin(facet, getPluginSetupBySourceSet(sourceSetNode),
                                         compilerPluginId = compilerPluginId,
                                         pluginName = pluginName)
    }

    override fun importByModule(facet: KotlinFacet, moduleNode: DataNode<ModuleData>) {
        modifyCompilerArgumentsForPlugin(facet, getPluginSetupByModule(moduleNode),
                                         compilerPluginId = compilerPluginId,
                                         pluginName = pluginName)
    }

    protected open fun getAnnotationsForPreset(presetName: String): List<String> = emptyList()

    protected open fun getAdditionalOptions(model: T): List<PluginOption> = emptyList()

    private fun getPluginSetupByModule(
            moduleNode: DataNode<ModuleData>
    ): CompilerPluginSetup? {
        val pluginModel = moduleNode.getCopyableUserData(modelKey)?.takeIf { it.isEnabled } ?: return null
        val annotations = pluginModel.annotations
        val presets = pluginModel.presets

        val allAnnotations = annotations + presets.flatMap { getAnnotationsForPreset(it) }
        val options = allAnnotations.map { PluginOption(annotationOptionName, it) } + getAdditionalOptions(pluginModel)

        // For now we can't use plugins from Gradle cause they're shaded and may have an incompatible version.
        // So we use ones from the IDEA plugin.
        val classpath = listOf(pluginJarFileFromIdea.absolutePath)

        return CompilerPluginSetup(options, classpath)
    }

    private fun getPluginSetupBySourceSet(sourceSetNode: DataNode<GradleSourceSetData>) =
            ExternalSystemApiUtil.findParent(sourceSetNode, ProjectKeys.MODULE)?.let { getPluginSetupByModule(it) }
}

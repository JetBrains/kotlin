/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.annotation.plugin.ide

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import org.jetbrains.kotlin.idea.configuration.GradleProjectImportHandler
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import java.io.File

abstract class AbstractGradleImportHandler : GradleProjectImportHandler {
    private companion object {
        private val TASK_DESCRIPTION_REGEX = "Supported annotations: ([^; ]*); (?:Presets: (.*?); )?Compiler plugin classpath: (.*?)".toRegex()
    }

    abstract val compilerPluginId: String
    abstract val pluginName: String
    abstract val annotationOptionName: String
    abstract val dataStorageTaskName: String
    abstract val pluginJarFileFromIdea: File

    override fun importBySourceSet(facet: KotlinFacet, sourceSetNode: DataNode<GradleSourceSetData>) {
        modifyCompilerArgumentsForPlugin(facet, getPluginSetupBySourceSet(sourceSetNode),
                                         compilerPluginId = compilerPluginId,
                                         pluginName = pluginName,
                                         annotationOptionName = annotationOptionName)
    }

    override fun importByModule(facet: KotlinFacet, moduleNode: DataNode<ModuleData>) {
        modifyCompilerArgumentsForPlugin(facet, getPluginSetupByModule(moduleNode),
                                         compilerPluginId = compilerPluginId,
                                         pluginName = pluginName,
                                         annotationOptionName = annotationOptionName)
    }

    protected open fun getAnnotationsForPreset(presetName: String): List<String> = emptyList()

    private fun getPluginSetupByModule(
            moduleNode: DataNode<ModuleData>
    ): AnnotationBasedCompilerPluginSetup? {
        val dataStorageTaskData = moduleNode.children.firstOrNull {
            val data = it.data as? TaskData ?: return@firstOrNull false
            data.name == dataStorageTaskName
        }?.data as? TaskData ?: return null

        val dataStorageTaskDescription = dataStorageTaskData.description ?: return null
        val matchResult = TASK_DESCRIPTION_REGEX.matchEntire(dataStorageTaskDescription)?.groupValues?.drop(1) ?: return null
        val (annotationFqNamesList, presets) = matchResult

        val annotationFqNames = annotationFqNamesList.split(',') + presets.split(',').flatMap { getAnnotationsForPreset(it) }

        // For now we can't use plugins from Gradle cause they're shaded and may have an incompatible version.
        // So we use ones from the IDEA plugin.
        val classpath = listOf(pluginJarFileFromIdea.absolutePath)

        return AnnotationBasedCompilerPluginSetup(annotationFqNames, classpath)
    }

    private fun getPluginSetupBySourceSet(sourceSetNode: DataNode<GradleSourceSetData>) =
            ExternalSystemApiUtil.findParent(sourceSetNode, ProjectKeys.MODULE)?.let { getPluginSetupByModule(it) }
}
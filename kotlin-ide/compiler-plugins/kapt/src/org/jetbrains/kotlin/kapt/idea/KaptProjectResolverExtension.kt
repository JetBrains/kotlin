/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.kapt.idea

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

@Suppress("unused")
class KaptProjectResolverExtension : AbstractProjectResolverExtension() {
    private companion object {
        private val LOG = Logger.getInstance(KaptProjectResolverExtension::class.java)
    }

    override fun getExtraProjectModelClasses() = setOf(KaptGradleModel::class.java)
    override fun getToolingExtensionsClasses() = setOf(KaptModelBuilderService::class.java, Unit::class.java)

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val kaptModel = resolverCtx.getExtraProject(gradleModule, KaptGradleModel::class.java)

        if (kaptModel != null && kaptModel.isEnabled) {
            for (sourceSet in kaptModel.sourceSets) {
                val parentDataNode = ideModule.findParentForSourceSetDataNode(sourceSet.sourceSetName) ?: continue

                fun addSourceSet(path: String, type: ExternalSystemSourceType) {
                    val contentRootData = ContentRootData(GRADLE_SYSTEM_ID, path)
                    contentRootData.storePath(type, path)
                    parentDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRootData)
                }

                val sourceType =
                    if (sourceSet.isTest) ExternalSystemSourceType.TEST_GENERATED else ExternalSystemSourceType.SOURCE_GENERATED
                sourceSet.generatedSourcesDirFile?.let { addSourceSet(it.absolutePath, sourceType) }
                sourceSet.generatedKotlinSourcesDirFile?.let { addSourceSet(it.absolutePath, sourceType) }

                sourceSet.generatedClassesDirFile?.let { generatedClassesDir ->
                    val libraryData = LibraryData(GRADLE_SYSTEM_ID, "kaptGeneratedClasses")
                    val existingNode =
                        parentDataNode.children.map { (it.data as? LibraryDependencyData)?.target }
                            .firstOrNull { it?.externalName == libraryData.externalName }
                    if (existingNode != null) {
                        existingNode.addPath(LibraryPathType.BINARY, generatedClassesDir.absolutePath)
                    } else {
                        libraryData.addPath(LibraryPathType.BINARY, generatedClassesDir.absolutePath)
                        val libraryDependencyData = LibraryDependencyData(parentDataNode.data, libraryData, LibraryLevel.MODULE)
                        parentDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData)
                    }
                }
            }
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }

    private fun DataNode<ModuleData>.findParentForSourceSetDataNode(sourceSetName: String): DataNode<ModuleData>? {
        val moduleName = data.id
        for (child in children) {
            val gradleSourceSetData = child.data as? GradleSourceSetData ?: continue
            if (gradleSourceSetData.id == "$moduleName:$sourceSetName") {
                @Suppress("UNCHECKED_CAST")
                return child as? DataNode<ModuleData>
            }
        }

        return this
    }
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle

import com.intellij.ide.actions.CreateDirectoryCompletionContributor
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ContentRootData
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiDirectory
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleUtil.findGradleModuleData

class GradleDirectoryCompletionContributor : CreateDirectoryCompletionContributor {
  override fun getDescription() = "Gradle Source Sets"

  override fun getVariants(directory: PsiDirectory): Collection<CreateDirectoryCompletionContributor.Variant> {
    val project = directory.project

    val module = ProjectFileIndex.getInstance(project).getModuleForFile(directory.virtualFile) ?: return emptyList()

    val moduleProperties = ExternalSystemModulePropertyManager.getInstance(module)
    if (GradleConstants.SYSTEM_ID.id != moduleProperties.getExternalSystemId()) return emptyList()

    val result = mutableListOf<CreateDirectoryCompletionContributor.Variant>()

    fun addAll(data: ContentRootData, type: ExternalSystemSourceType, rootType: JpsModuleSourceRootType<*>) {
      result.addAll(data.getPaths(type).map { CreateDirectoryCompletionContributor.Variant(it.path, rootType) })
    }

    fun addAll(moduleData: DataNode<out ModuleData>) {
      for (eachContentRootNode in ExternalSystemApiUtil.findAll(moduleData, ProjectKeys.CONTENT_ROOT)) {
        addAll(eachContentRootNode.data, ExternalSystemSourceType.SOURCE, JavaSourceRootType.SOURCE)
        addAll(eachContentRootNode.data, ExternalSystemSourceType.TEST, JavaSourceRootType.TEST_SOURCE)
        addAll(eachContentRootNode.data, ExternalSystemSourceType.RESOURCE, JavaResourceRootType.RESOURCE)
        addAll(eachContentRootNode.data, ExternalSystemSourceType.TEST_RESOURCE, JavaResourceRootType.TEST_RESOURCE)
      }
    }

    val moduleData = findGradleModuleData(module) ?: return emptyList()
    addAll(moduleData)
    for (eachSourceSetNode in ExternalSystemApiUtil.getChildren(moduleData, GradleSourceSetData.KEY)) {
      addAll(eachSourceSetNode)
    }

    return result;
  }
}

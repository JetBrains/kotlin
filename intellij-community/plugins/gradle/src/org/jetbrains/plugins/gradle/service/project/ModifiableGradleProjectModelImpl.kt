// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.util.GradleConstants

@ApiStatus.Internal
class ModifiableGradleProjectModelImpl(private val projectName: String, private val projectPath: String) : ModifiableGradleProjectModel {
  private val projectModels = lazy { mutableMapOf<Key<Any>, MutableList<Any>>() }

  override fun <T> addProjectData(key: Key<T>, data: T): ModifiableGradleProjectModel {
    @Suppress("UNCHECKED_CAST")
    projectModels.value.computeIfAbsent(key as Key<Any>) { mutableListOf() }.add(data as Any)
    return this
  }

  fun buildDataNodeGraph(): DataNode<ProjectData> {
    val projectData = ProjectData(GradleConstants.SYSTEM_ID, projectName, projectPath, projectPath)
    val projectDataNode = DataNode(ProjectKeys.PROJECT, projectData, null)
    if (projectModels.isInitialized()) {
      projectModels.value.forEach { (key, list) ->
        list.forEach { projectDataNode.createChild(key, it) }
      }
    }
    return projectDataNode
  }
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.util.xmlb.annotations.Attribute

@State(name = "ProjectId", storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))])
internal class ProjectIdManager : PersistentStateComponent<ProjectIdState>, ModificationTracker {
  companion object {
    fun getInstance(project: Project) = project.service<ProjectIdManager>()
  }

  private var state = ProjectIdState()

  override fun getState() = state

  override fun loadState(state: ProjectIdState) {
    this.state = state
  }

  override fun getModificationCount() = state.modificationCount
}

internal class ProjectIdState : BaseState() {
  @get:Attribute
  var id by string()
}

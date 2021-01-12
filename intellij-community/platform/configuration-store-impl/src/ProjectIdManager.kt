// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute

@State(name = "ProjectId", storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))], reportStatistic = false)
internal class ProjectIdManager : SimplePersistentStateComponent<ProjectIdState>(ProjectIdState()) {
  companion object {
    fun getInstance(project: Project) = project.service<ProjectIdManager>()
  }
}

internal class ProjectIdState : BaseState() {
  @get:Attribute
  var id by string()
}

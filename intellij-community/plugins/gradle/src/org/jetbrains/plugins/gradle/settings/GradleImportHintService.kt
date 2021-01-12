// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.XCollection

class GradleImportHint : BaseState() {
  @get:XCollection
  val projectsToImport by list<String>()
  val skip by property(false)
}

class GradleImportHintService : SimplePersistentStateComponent<GradleImportHint>(GradleImportHint()) {
  companion object {
    fun getInstance(project: Project) : GradleImportHintService = project.service()
  }
}

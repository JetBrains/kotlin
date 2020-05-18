// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil.copyBean

@State(name = "ProblemsViewState", storages = [(Storage(value = StoragePathMacros.WORKSPACE_FILE))])
class ProblemsViewState : PersistentStateComponent<ProblemsViewState> {
  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProblemsViewState = project.service()
  }

  var selectedIndex = 0
  var proportion = 0.5f

  var autoscrollToSource = false
  var showPreview = false
  var showToolbar = true

  var showErrors = true
  var showWarnings = true
  var showInformation = true

  var sortFoldersFirst = true
  var sortBySeverity = true
  var sortByName = false

  override fun getState(): ProblemsViewState? = this

  override fun loadState(state: ProblemsViewState) = copyBean(state, this)

  override fun noStateLoaded() {
    autoscrollToSource = UISettings.instance.state.defaultAutoScrollToSource
  }
}

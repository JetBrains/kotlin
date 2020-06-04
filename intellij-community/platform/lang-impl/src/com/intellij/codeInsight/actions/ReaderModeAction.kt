// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.actions

import com.intellij.codeInsight.actions.ReaderModeFileEditorListener.Companion.matchMode
import com.intellij.lang.LangBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.Experiments

class ReaderModeActionGroup : DefaultActionGroup()

class ReaderModeAction : ToggleAction() {
  override fun update(e: AnActionEvent) {
    if (!Experiments.getInstance().isFeatureEnabled("editor.reader.mode")) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    super.update(e)
    val project = e.project
    val file = PlatformDataKeys.VIRTUAL_FILE.getData(e.dataContext)

    e.presentation.isEnabledAndVisible = matchMode(project, file)
    e.presentation.text = LangBundle.message("action.ReaderMode.float.text")
  }

  override fun setSelected(e: AnActionEvent, readerMode: Boolean) {
    val project = e.project ?: return

    ReaderModeSettings.instance(project).enabled = readerMode
    project.messageBus.syncPublisher(READER_MODE_TOPIC).modeChanged(project)
  }

  override fun isSelected(e: AnActionEvent): Boolean {
    return ReaderModeSettings.instance(e.project ?: return false).enabled
  }
}
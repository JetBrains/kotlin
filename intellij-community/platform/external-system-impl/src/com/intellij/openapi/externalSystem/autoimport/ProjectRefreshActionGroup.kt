// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diff.impl.DiffUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.testFramework.LightVirtualFileBase

class ProjectRefreshActionGroup : DefaultActionGroup() {
  override fun update(e: AnActionEvent) {
    ensureValidActionVisibility(e)
    val project = e.project ?: return
    val toolbarComponent = e.toolbarComponent ?: return
    val notificationAware = ProjectNotificationAware.getInstance(project)
    when (notificationAware.isNotificationVisible()) {
      true -> toolbarComponent.scheduleShow()
      else -> toolbarComponent.scheduleHide()
    }
  }

  private fun ensureValidActionVisibility(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    when {
      DiffUtil.isDiffEditor(editor) -> e.presentation.isVisible = false
      !editor.isFileEditor() -> e.presentation.isVisible = false
    }
  }

  private fun Editor.isFileEditor(): Boolean {
    val documentManager = FileDocumentManager.getInstance()
    val virtualFile = documentManager.getFile(document)
    if (virtualFile is LightVirtualFileBase) return false
    return virtualFile != null && virtualFile.isValid
  }

  private val AnActionEvent.toolbarComponent: FloatingToolbarComponent?
    get() = getData(FloatingToolbarComponent.KEY)
}
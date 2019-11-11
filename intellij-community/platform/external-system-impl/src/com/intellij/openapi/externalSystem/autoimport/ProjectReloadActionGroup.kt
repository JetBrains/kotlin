// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent
import com.intellij.openapi.fileEditor.FileDocumentManager

class ProjectReloadActionGroup : DefaultActionGroup() {
  override fun update(e: AnActionEvent) {
    val path = e.currentFilePath ?: return
    val project = e.project ?: return
    val toolbarComponent = e.toolbarComponent ?: return
    val notificationAware = ProjectNotificationAware.getInstance(project)
    when (notificationAware.isNotificationVisibleIn(path)) {
      true -> toolbarComponent.scheduleShow()
      else -> toolbarComponent.scheduleHide()
    }
  }

  private val AnActionEvent.currentFilePath: String?
    get() {
      val editor = getData(CommonDataKeys.EDITOR) ?: return null
      val fileManager = FileDocumentManager.getInstance()
      val virtualFile = fileManager.getFile(editor.document) ?: return null
      return virtualFile.path
    }

  private val AnActionEvent.toolbarComponent: FloatingToolbarComponent?
    get() = getData(FloatingToolbarComponent.KEY)
}
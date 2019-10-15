// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.ui.DefaultExternalSystemIconProvider
import com.intellij.openapi.externalSystem.ui.ExternalSystemIconProvider
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.text.NaturalComparator
import javax.swing.Icon

class ProjectReloadActionGroup : DefaultActionGroup() {

  override fun update(e: AnActionEvent) {
    val path = e.currentFilePath ?: return
    val notificationAware = e.notificationAware ?: return
    e.presentation.isVisible = notificationAware.isNotificationVisibleIn(path)
  }

  class ProjectRefreshAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val project = e.project ?: return
      val projectTracker = ExternalSystemProjectTracker.getInstance(project)
      projectTracker.scheduleProjectRefresh()
    }

    override fun update(e: AnActionEvent) {
      val notificationAware = e.notificationAware ?: return
      val systemIds = notificationAware.getSystemIds()
      e.presentation.text = getNotificationText(systemIds)
      e.presentation.description = getNotificationDescription(systemIds)
      e.presentation.icon = getNotificationIcon(systemIds)
    }

    private fun getNotificationText(systemIds: Set<ProjectSystemId>): String {
      val systemsPresentation = systemIds.joinToString { it.readableName }
      return ExternalSystemBundle.message("external.system.reload.notification.action.reload.text", systemsPresentation)
    }

    private fun List<String>.naturalJoin(): String {
      if (size == 0) return ""
      if (size == 1) return first()
      val leading = dropLast(1).joinToString(", ")
      return ExternalSystemBundle.message("external.system.reload.notification.action.reload.and.conjunction", leading, last())
    }

    private fun getNotificationDescription(systemIds: Set<ProjectSystemId>): String {
      val systemsPresentation = systemIds.map { it.readableName }
        .sortedWith(NaturalComparator.INSTANCE)
        .naturalJoin()
      val productName = ApplicationNamesInfo.getInstance().fullProductName
      return ExternalSystemBundle.message("external.system.reload.notification.action.reload.description", systemsPresentation, productName)
    }

    private fun getNotificationIcon(systemIds: Set<ProjectSystemId>): Icon {
      val iconManager = when (systemIds.size) {
        1 -> ExternalSystemIconProvider.getExtension(systemIds.first())
        else -> DefaultExternalSystemIconProvider
      }
      return iconManager.reloadIcon
    }
  }

  class HideProjectRefreshAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val notificationAware = e.notificationAware ?: return
      notificationAware.hideNotification()
    }

    init {
      templatePresentation.text = ExternalSystemBundle.message("external.system.reload.notification.action.hide.text")
      templatePresentation.icon = AllIcons.Actions.Close
      templatePresentation.hoveredIcon = AllIcons.Actions.CloseHovered
    }
  }

  companion object {
    private val AnActionEvent.notificationAware: ProjectNotificationAware?
      get() = project?.let { ProjectNotificationAware.getInstance(it) }

    private val AnActionEvent.currentFilePath: String?
      get() {
        val editor = getData(CommonDataKeys.EDITOR) ?: return null
        val fileManager = FileDocumentManager.getInstance()
        val virtualFile = fileManager.getFile(editor.document) ?: return null
        return virtualFile.path
      }
  }
}
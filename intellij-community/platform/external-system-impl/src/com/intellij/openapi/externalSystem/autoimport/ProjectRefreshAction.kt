// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.ui.DefaultExternalSystemIconProvider
import com.intellij.openapi.externalSystem.ui.ExternalSystemIconProvider
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.text.NaturalComparator
import javax.swing.Icon

class ProjectRefreshAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val projectTracker = ExternalSystemProjectTracker.getInstance(project)
    projectTracker.scheduleProjectRefresh()
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    val notificationAware = ProjectNotificationAware.getInstance(project)
    val systemIds = notificationAware.getSystemIds()
    if (systemIds.isNotEmpty()) {
      e.presentation.text = getNotificationText(systemIds)
      e.presentation.description = getNotificationDescription(systemIds)
      e.presentation.icon = getNotificationIcon(systemIds)
    }
    e.presentation.isEnabled = notificationAware.isNotificationVisible()
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

  init {
    val productName = ApplicationNamesInfo.getInstance().fullProductName
    templatePresentation.icon = DefaultExternalSystemIconProvider.reloadIcon
    templatePresentation.text = ExternalSystemBundle.message("external.system.reload.notification.action.reload.text.empty")
    templatePresentation.description = ExternalSystemBundle.message("external.system.reload.notification.action.reload.description.empty", productName)
  }
}
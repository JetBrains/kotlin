// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly

class ProjectNotificationAware : Disposable {
  private var isHidden = false
  private val projectsWithNotification = HashSet<ExternalSystemProjectId>()

  fun notificationNotify(projectAware: ExternalSystemProjectAware) = runInEdt {
    val projectId = projectAware.projectId
    LOG.debug("${projectId.readableName}: Notify notification")
    projectsWithNotification.add(projectId)
    revealNotification()
  }

  fun notificationExpire(projectId: ExternalSystemProjectId) = runInEdt {
    LOG.debug("${projectId.readableName}: Expire notification")
    projectsWithNotification.remove(projectId)
    revealNotification()
  }

  fun notificationExpire() = runInEdt {
    LOG.debug("Expire notification")
    projectsWithNotification.clear()
    revealNotification()
  }

  override fun dispose() {
    notificationExpire()
  }

  private fun setHideStatus(isHidden: Boolean) = runInEdt {
    this.isHidden = isHidden
    ApplicationManager.getApplication().assertIsDispatchThread()
    val toolbarProvider = ProjectRefreshFloatingProvider.getExtension()
    toolbarProvider.updateAllToolbarComponents()
  }

  private fun revealNotification() = setHideStatus(false)

  fun hideNotification() = setHideStatus(true)

  fun isNotificationVisible(): Boolean {
    ApplicationManager.getApplication().assertIsDispatchThread()
    return !isHidden && projectsWithNotification.isNotEmpty()
  }

  fun getSystemIds(): Set<ProjectSystemId> {
    ApplicationManager.getApplication().assertIsDispatchThread()
    return projectsWithNotification.map { it.systemId }.toSet()
  }

  @TestOnly
  fun getProjectsWithNotification(): Set<ExternalSystemProjectId> {
    ApplicationManager.getApplication().assertIsDispatchThread()
    return projectsWithNotification.toSet()
  }

  companion object {
    private val LOG = Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport")

    @JvmStatic
    fun getInstance(project: Project) = project.service<ProjectNotificationAware>()
  }
}
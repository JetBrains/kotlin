// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.notification.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import gnu.trove.THashSet
import org.jetbrains.annotations.TestOnly

class ProjectNotificationAware(private val project: Project) : Disposable {

  private var notification: Notification? = null

  private val projectsWithNotification = THashSet<ExternalSystemProjectId>()

  private val content: String
    get() = projectsWithNotification.map { it.systemId.readableName }.toSet().joinToString() + " project need to be imported"

  fun notificationNotify(projectAware: ExternalSystemProjectAware) = runInEdt {
    val projectId = projectAware.projectId
    LOG.debug("${projectId.readableName}: Notify notification")
    projectsWithNotification.add(projectId)
    notificationUpdate()
  }

  fun notificationExpire(projectId: ExternalSystemProjectId) = runInEdt {
    LOG.debug("${projectId.readableName}: Expire notification")
    projectsWithNotification.remove(projectId)
    notificationUpdate()
  }

  fun notificationExpire() = runInEdt {
    LOG.debug("Expire notification")
    projectsWithNotification.clear()
    notificationUpdate()
  }

  override fun dispose() {
    notificationExpire()
  }

  @TestOnly
  fun isNotificationNotified(vararg projects: ExternalSystemProjectId) =
    notification != null && projectsWithNotification.containsAll(projects.toList())

  private fun notificationUpdate() {
    when {
      projectsWithNotification.isEmpty -> {
        notification?.expire()
        notification = null
        LOG.debug("Notification expired")
      }
      notification == null -> {
        notification = NOTIFICATION_GROUP.createNotification(content, NotificationType.INFORMATION)
        notification?.addAction(NotificationAction.createSimple("Import Changes") {
          notificationExpire()
          ExternalSystemProjectTracker.getInstance(project).scheduleProjectRefresh()
        })
        notification?.notify(project)
        LOG.debug("Notification notified")
      }
      else -> {
        notification?.setContent(content)
        LOG.debug("Notification updated")
      }
    }
  }

  companion object {
    private val LOG = Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport")

    private val NOTIFICATION_GROUP = NotificationGroup("External System Update", NotificationDisplayType.STICKY_BALLOON, true)

    fun getInstance(project: Project): ProjectNotificationAware {
      return ServiceManager.getService(project, ProjectNotificationAware::class.java)
    }
  }
}
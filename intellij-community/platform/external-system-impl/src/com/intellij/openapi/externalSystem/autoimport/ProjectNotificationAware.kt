// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.notification.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly

class ProjectNotificationAware(private val project: Project) : ExternalSystemProjectNotificationAware {

  private var notification: Notification? = null

  private val projectAwareMap = LinkedHashMap<ExternalSystemProjectId, ExternalSystemProjectAware>()

  private val content: String
    get() = projectAwareMap.keys.map { it.systemId.readableName }.toSet().joinToString() + " project need to be imported"

  override fun notificationNotify(projectAware: ExternalSystemProjectAware) {
    val projectId = projectAware.projectId
    LOG.debug("${projectId.readableName}: Notify notification")
    synchronized(this) {
      projectAwareMap[projectId] = projectAware
      notificationUpdate()
    }
  }

  override fun notificationExpire(projectId: ExternalSystemProjectId) {
    LOG.debug("${projectId.readableName}: Expire notification")
    synchronized(this) {
      projectAwareMap.remove(projectId)
      notificationUpdate()
    }
  }

  override fun notificationExpire() {
    LOG.debug("Expire notification")
    synchronized(this) {
      projectAwareMap.clear()
      notificationUpdate()
    }
  }

  override fun dispose() {
    notificationExpire()
  }

  @TestOnly
  fun isNotificationNotified(vararg projects: ExternalSystemProjectId) =
    synchronized(this) {
      notification != null && projectAwareMap.keys.containsAll(projects.toList())
    }

  private fun notificationUpdate() {
    when {
      projectAwareMap.isEmpty() -> {
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
  }
}
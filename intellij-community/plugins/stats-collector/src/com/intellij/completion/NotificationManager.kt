// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion

import com.intellij.completion.settings.CompletionStatsCollectorSettings
import com.intellij.ide.util.PropertiesComponent
import com.intellij.internal.statistic.utils.StatisticsUploadAssistant
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class NotificationManager : StartupActivity {
  companion object {
    private const val MESSAGE_SHOWN_KEY = "completion.stats.allow.question.shown"
  }

  private fun isMessageShown() = PropertiesComponent.getInstance().getBoolean(MESSAGE_SHOWN_KEY, false)

  private fun fireMessageShown() = PropertiesComponent.getInstance().setValue(MESSAGE_SHOWN_KEY, true)

  override fun runActivity(project: Project) {
    val application = ApplicationManager.getApplication()
    if (application.isUnitTestMode) {
      return
    }

    // Show message once only in EAP builds if user allows to send statistics
    if (application.isEAP && StatisticsUploadAssistant.isSendAllowed() && !isMessageShown()) {
      notify(project)
      fireMessageShown()
    }
  }

  private fun notify(project: Project) {
    val pluginName = StatsCollectorBundle.message("completion.stats.plugin.name.in.notification")
    val messageText = StatsCollectorBundle.message("completion.stats.notification.text")
    val notification = Notification(pluginName, pluginName, messageText, NotificationType.INFORMATION)
      .addAction(object : NotificationAction("Allow") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          CompletionStatsCollectorSettings.getInstance().setCompletionLogsSendAllowed(true)
          notification.expire()
        }
      })
      .addAction(object : NotificationAction("Deny") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          CompletionStatsCollectorSettings.getInstance().setCompletionLogsSendAllowed(false)
          notification.expire()
        }
      })
    notification.notify(project)
  }
}
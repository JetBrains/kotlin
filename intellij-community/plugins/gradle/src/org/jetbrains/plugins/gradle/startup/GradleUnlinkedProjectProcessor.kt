// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.startup

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.plugins.gradle.config.GradleSettingsListenerAdapter
import org.jetbrains.plugins.gradle.service.project.GradleNotification
import org.jetbrains.plugins.gradle.service.project.open.linkAndRefreshGradleProject
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.GradleConstants

class GradleUnlinkedProjectProcessor : StartupActivity.DumbAware {

  override fun runActivity(project: Project) {
    if (isEnabledNotifications(project)) {
      showNotification(project)
    }
  }

  companion object {
    private const val SHOW_UNLINKED_GRADLE_POPUP = "show.unlinked.gradle.project.popup"

    private val LOG = Logger.getInstance(GradleUnlinkedProjectProcessor::class.java)

    private fun showNotification(project: Project) {
      if (!GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()) return
      if (project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) === java.lang.Boolean.TRUE) return

      val externalProjectPath = project.basePath ?: return
      val gradleGroovyDslFile = externalProjectPath + "/" + GradleConstants.DEFAULT_SCRIPT_NAME
      val kotlinDslGradleFile = externalProjectPath + "/" + GradleConstants.KOTLIN_DSL_SCRIPT_NAME
      if (FileUtil.findFirstThatExist(gradleGroovyDslFile, kotlinDslGradleFile) == null) return


      val subscription = Disposer.newDisposable()
      val notification = GradleNotification.NOTIFICATION_GROUP.createNotification(
        GradleBundle.message("gradle.notifications.unlinked.project.found.title", ApplicationNamesInfo.getInstance().fullProductName),
        NotificationType.INFORMATION)

      val notificationExpire = {
        notification.expire()
        LOG.debug("Unlinked project notification expired")
        Disposer.dispose(subscription)
      }

      notification.addAction(NotificationAction.createSimple(
        GradleBundle.message("gradle.notifications.unlinked.project.found.import")) {
        notificationExpire()
        linkAndRefreshGradleProject(externalProjectPath, project)
      })
      notification.addAction(NotificationAction.createSimple(
        GradleBundle.message("gradle.notifications.unlinked.project.found.skip")) {
        notificationExpire()
        disableNotifications(project)
      })

      notification.contextHelpAction = object : DumbAwareAction(
        "Help", GradleBundle.message("gradle.notifications.unlinked.project.found.help"), null) {
        override fun actionPerformed(e: AnActionEvent) {}
      }

      val settingsListener = object : GradleSettingsListenerAdapter() {
        override fun onProjectsLinked(settings: MutableCollection<GradleProjectSettings>) {
          notificationExpire()
        }
      }
      Disposer.register(project, subscription)
      ExternalSystemApiUtil.subscribe(project, GradleConstants.SYSTEM_ID, settingsListener, subscription)

      notification.notify(project)
    }

    private fun isEnabledNotifications(project: Project): Boolean {
      return PropertiesComponent.getInstance(project).getBoolean(SHOW_UNLINKED_GRADLE_POPUP, true)
    }

    private fun disableNotifications(project: Project) {
      PropertiesComponent.getInstance(project).setValue(SHOW_UNLINKED_GRADLE_POPUP, false, true)
    }

    fun enableNotifications(project: Project) {
      PropertiesComponent.getInstance(project).setValue(SHOW_UNLINKED_GRADLE_POPUP, true, false)
    }
  }
}
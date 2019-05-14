// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.startup

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.plugins.gradle.service.project.GradleNotification
import org.jetbrains.plugins.gradle.service.project.open.importProject
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.GradleConstants
import javax.swing.event.HyperlinkEvent

class GradleUnlinkedProjectProcessor : StartupActivity {

  override fun runActivity(project: Project) {
    if (isEnabledNotifications(project)) {
      showNotification(project)
    }
  }

  companion object {
    private const val SHOW_UNLINKED_GRADLE_POPUP = "show.inlinked.gradle.project.popup"
    private const val IMPORT_EVENT_DESCRIPTION = "import"
    private const val DO_NOT_SHOW_EVENT_DESCRIPTION = "do.not.show"

    private fun showNotification(project: Project) {
      if (!GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()) return
      if (project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) === java.lang.Boolean.TRUE) return

      val externalProjectPath = project.basePath ?: return
      val gradleGroovyDslFile = externalProjectPath + "/" + GradleConstants.DEFAULT_SCRIPT_NAME
      val kotlinDslGradleFile = externalProjectPath + "/" + GradleConstants.KOTLIN_DSL_SCRIPT_NAME
      if (FileUtil.findFirstThatExist(gradleGroovyDslFile, kotlinDslGradleFile) == null) return

      val message = String.format("%s<br>\n%s",
                                  GradleBundle.message("gradle.notifications.unlinked.project.found.msg", IMPORT_EVENT_DESCRIPTION),
                                  GradleBundle.message("gradle.notifications.do.not.show"))
      val hyperLinkAdapter = object : NotificationListener.Adapter() {
        override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
          notification.expire()
          when (e.description) {
            IMPORT_EVENT_DESCRIPTION -> importProject(externalProjectPath, project)
            DO_NOT_SHOW_EVENT_DESCRIPTION -> disableNotifications(project)
          }
        }
      }
      GradleNotification.getInstance(project).showBalloon(
        GradleBundle.message("gradle.notifications.unlinked.project.found.title"),
        message, NotificationType.INFORMATION, hyperLinkAdapter
      )
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
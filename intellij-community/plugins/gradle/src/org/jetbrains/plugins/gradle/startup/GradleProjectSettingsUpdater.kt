// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.startup

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.ExternalSystemConfigurableAware
import com.intellij.openapi.externalSystem.ExternalSystemManager
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkProvider
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalSystemConfigurable
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListenerEx
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.SdkListPresenter
import org.jetbrains.plugins.gradle.GradleManager
import org.jetbrains.plugins.gradle.service.project.GradleNotification.NOTIFICATION_GROUP
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.getGradleJvmLookupProvider
import org.jetbrains.plugins.gradle.util.isSupported

class GradleProjectSettingsUpdater : ExternalSystemSettingsListenerEx {
  override fun onProjectsLoaded(
    project: Project,
    manager: ExternalSystemManager<*, *, *, *, *>,
    settings: Collection<ExternalProjectSettings>
  ) {
    /**
     * 1. Internal JDK can be uses for tests
     * 2. Some tests creates fake SDKs. Those SDKs must not be replaced
     */
    if (ApplicationManager.getApplication().isUnitTestMode) return

    if (manager !is GradleManager) return
    for (projectSettings in settings) {
      if (projectSettings !is GradleProjectSettings) continue
      updateGradleJvm(project, projectSettings)
    }
  }

  private fun updateGradleJvm(project: Project, projectSettings: GradleProjectSettings) {
    val gradleJvm = projectSettings.gradleJvm ?: return
    when {
      isInternalSdk(gradleJvm) -> fixupInternalJdk(project, projectSettings)
      isSdkName(gradleJvm) && isUnknownSdk(gradleJvm) -> fixupUnknownSdk(project, projectSettings)
    }
  }

  private fun fixupInternalJdk(project: Project, projectSettings: GradleProjectSettings) {
    val jdkProvider = ExternalSystemJdkProvider.getInstance()
    val internalSdk = jdkProvider.internalJdk
    projectSettings.gradleJvm = internalSdk.name
    fixupUnknownSdk(project, projectSettings)
  }

  private fun fixupUnknownSdk(project: Project, projectSettings: GradleProjectSettings) {
    val gradleJvm = projectSettings.gradleJvm ?: return
    val gradleVersion = projectSettings.resolveGradleVersion()

    projectSettings.gradleJvm = null
    getGradleJvmLookupProvider(project, projectSettings)
      .newLookupBuilder()
      .withSdkName(gradleJvm)
      .withVersionFilter { isSupported(gradleVersion, it) }
      .withSdkType(ExternalSystemJdkUtil.getJavaSdkType())
      .withSdkHomeFilter { ExternalSystemJdkUtil.isValidJdk(it) }
      .onSdkNameResolved { sdk ->
        val fakeSdk = sdk?.let(::findRegisteredSdk)
        if (fakeSdk != null && projectSettings.gradleJvm == null) {
          projectSettings.gradleJvm = fakeSdk.name
        }
      }
      .onSdkResolved { sdk ->
        if (projectSettings.gradleJvm == null) {
          projectSettings.gradleJvm = sdk?.name ?: gradleJvm
        }
        notifyGradleJvmChangeInfo(project, projectSettings, gradleJvm, sdk)
      }
      .executeLookup()
  }

  private fun notifyGradleJvmChangeInfo(
    project: Project,
    projectSettings: GradleProjectSettings,
    gradleJvm: String,
    sdk: Sdk?
  ) {
    if (sdk == null) return
    val versionString = sdk.versionString ?: return
    val homePath = sdk.homePath ?: return
    val externalProjectPath = projectSettings.externalProjectPath ?: return

    val presentablePath = SdkListPresenter.presentDetectedSdkPath(homePath)
    val notificationTitle = GradleBundle.message("gradle.notifications.java.home.change.title")
    val notificationContent = GradleBundle.message("gradle.notifications.java.home.change.content", gradleJvm, versionString,
                                                   presentablePath)
    val notification = NOTIFICATION_GROUP.createNotification(notificationTitle, notificationContent, INFORMATION)
    notification.addAction(NotificationAction.createSimple(GradleBundle.message("gradle.open.gradle.settings")) {
      showGradleProjectSettings(project, externalProjectPath)
    })
    notification.notify(project)
  }

  private fun showGradleProjectSettings(project: Project, externalProjectPath: String) {
    val manager = ExternalSystemApiUtil.getManager(GradleConstants.SYSTEM_ID)
    val configurable = (manager as ExternalSystemConfigurableAware).getConfigurable(project)
    if (configurable is AbstractExternalSystemConfigurable<*, *, *>) {
      val settingsUtil = ShowSettingsUtil.getInstance()
      settingsUtil.editConfigurable(project, configurable) {
        configurable.selectProject(externalProjectPath)
      }
    }
  }

  private fun isUnknownSdk(sdkName: String): Boolean {
    val projectJdkTable = ProjectJdkTable.getInstance()
    val sdk = projectJdkTable.findJdk(sdkName)
    return sdk == null
  }

  private fun isSdkName(jdkReference: String?) =
    jdkReference != null && !jdkReference.startsWith('#')

  private fun isInternalSdk(jdkReference: String?) =
    jdkReference == ExternalSystemJdkUtil.USE_INTERNAL_JAVA

  private fun findRegisteredSdk(sdk: Sdk): Sdk? = runReadAction {
    val projectJdkTable = ProjectJdkTable.getInstance()
    projectJdkTable.findJdk(sdk.name, sdk.sdkType.name)
  }
}
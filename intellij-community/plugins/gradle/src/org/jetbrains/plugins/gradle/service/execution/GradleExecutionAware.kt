// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.execution

import com.intellij.build.events.impl.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskState.CANCELED
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskState.CANCELING
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemBuildEvent
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemExecutionAware
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkException
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.externalSystem.service.notification.callback.OpenExternalSystemSettingsCallback
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider.SdkInfo
import com.intellij.util.ConcurrencyUtil
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.GradleBundle.PATH_TO_BUNDLE
import org.jetbrains.plugins.gradle.util.getGradleJvmLookupProvider
import org.jetbrains.plugins.gradle.util.nonblockingResolveGradleJvmInfo
import java.lang.System.currentTimeMillis

class GradleExecutionAware : ExternalSystemExecutionAware {
  override fun prepareExecution(
    task: ExternalSystemTask,
    externalProjectPath: String,
    isPreviewMode: Boolean,
    taskNotificationListener: ExternalSystemTaskNotificationListener,
    project: Project
  ) {
    if (!isPreviewMode) prepareJvmForExecution(task, externalProjectPath, taskNotificationListener, project)
  }

  private fun prepareJvmForExecution(
    task: ExternalSystemTask,
    externalProjectPath: String,
    taskNotificationListener: ExternalSystemTaskNotificationListener,
    project: Project
  ) {
    val settings = use(project) { GradleSettings.getInstance(it) }
    val projectSettings = settings.getLinkedProjectSettings(externalProjectPath) ?: return
    val gradleJvm = projectSettings.gradleJvm

    val provider = use(project) { getGradleJvmLookupProvider(it, projectSettings) }
    var sdkInfo = use(project) { provider.nonblockingResolveGradleJvmInfo(it, externalProjectPath, gradleJvm) }
    if (sdkInfo is SdkInfo.Unresolved || sdkInfo is SdkInfo.Resolving) {
      provider.waitForGradleJvmResolving(task, taskNotificationListener)
      sdkInfo = use(project) { provider.nonblockingResolveGradleJvmInfo(it, externalProjectPath, gradleJvm) }
    }

    if (sdkInfo !is SdkInfo.Resolved) throw jdkConfigurationException("gradle.jvm.is.invalid")
    val homePath = sdkInfo.homePath ?: throw jdkConfigurationException("gradle.jvm.is.invalid")
    if (!JdkUtil.checkForJdk(homePath)) {
      if (JdkUtil.checkForJre(homePath)) {
        throw jdkConfigurationException("gradle.jvm.is.jre")
      }
      throw jdkConfigurationException("gradle.jvm.is.invalid")
    }
  }

  private fun <R> use(project: Project, action: (Project) -> R): R {
    return invokeAndWaitIfNeeded {
      runWriteAction {
        when (project.isDisposed) {
          true -> throw ProcessCanceledException()
          else -> action(project)
        }
      }
    }
  }

  private fun jdkConfigurationException(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String): ExternalSystemJdkException {
    val errorMessage = GradleBundle.message(key)
    val openSettingsMessage = GradleBundle.message("gradle.open.gradle.settings")
    val exceptionMessage = String.format("$errorMessage <a href='%s'>$openSettingsMessage</a> \n", OpenExternalSystemSettingsCallback.ID)
    return ExternalSystemJdkException(exceptionMessage, null, OpenExternalSystemSettingsCallback.ID)
  }

  private fun SdkLookupProvider.waitForGradleJvmResolving(
    task: ExternalSystemTask,
    taskNotificationListener: ExternalSystemTaskNotificationListener
  ): Sdk? {
    if (ApplicationManager.getApplication().isDispatchThread) {
      LOG.error("Do not perform synchronous wait for sdk downloading in EDT - causes deadlock.")
      throw jdkConfigurationException("gradle.jvm.is.being.resolved.error")
    }

    val progressIndicator = createProgressIndicator(task.id, taskNotificationListener)
    taskNotificationListener.submitProgressStarted(task.id, progressIndicator)
    onProgress(progressIndicator)
    task.onCancel { progressIndicator.cancel() }
    val result = blockingGetSdk()
    taskNotificationListener.submitProgressFinished(task.id, progressIndicator)
    return result
  }

  private fun ExternalSystemTask.onCancel(callback: () -> Unit) {
    val wrappedCallback = ConcurrencyUtil.once(callback)
    val progressManager = ExternalSystemProgressNotificationManager.getInstance()
    val notificationListener = object : ExternalSystemTaskNotificationListenerAdapter() {
      override fun onCancel(id: ExternalSystemTaskId) {
        wrappedCallback.run()
        progressManager.removeNotificationListener(this)
      }
    }
    progressManager.addNotificationListener(id, notificationListener)
    if (state == CANCELED || state == CANCELING) wrappedCallback.run()
  }

  private fun createProgressIndicator(
    taskId: ExternalSystemTaskId,
    taskNotificationListener: ExternalSystemTaskNotificationListener
  ): ProgressIndicator {
    return object : AbstractProgressIndicatorExBase() {
      override fun setFraction(fraction: Double) {
        super.setFraction(fraction)
        taskNotificationListener.submitProgressStatus(taskId, this)
      }
    }
  }

  private fun ExternalSystemTaskNotificationListener.submitProgressStarted(
    taskId: ExternalSystemTaskId,
    progressIndicator: ProgressIndicator
  ) {
    val message = progressIndicator.text ?: GradleBundle.message("gradle.jvm.is.being.resolved")
    val buildEvent = StartEventImpl(progressIndicator, taskId, currentTimeMillis(), message)
    val notificationEvent = ExternalSystemBuildEvent(taskId, buildEvent)
    onStatusChange(notificationEvent)
  }

  private fun ExternalSystemTaskNotificationListener.submitProgressFinished(
    taskId: ExternalSystemTaskId,
    progressIndicator: ProgressIndicator
  ) {
    val result = when {
      progressIndicator.isCanceled -> SkippedResultImpl()
      else -> SuccessResultImpl()
    }
    val message = progressIndicator.text ?: GradleBundle.message("gradle.jvm.has.been.resolved")
    val buildEvent = FinishEventImpl(progressIndicator, taskId, currentTimeMillis(), message, result)
    val notificationEvent = ExternalSystemBuildEvent(taskId, buildEvent)
    onStatusChange(notificationEvent)
  }

  private fun ExternalSystemTaskNotificationListener.submitProgressStatus(
    taskId: ExternalSystemTaskId,
    progressIndicator: ProgressIndicator
  ) {
    val progress = (progressIndicator.fraction * 100).toLong()
    val message = progressIndicator.text ?: GradleBundle.message("gradle.jvm.is.being.resolved")
    val buildEvent = ProgressBuildEventImpl(progressIndicator, taskId, currentTimeMillis(), message, 100, progress, "%")
    val notificationEvent = ExternalSystemBuildEvent(taskId, buildEvent)
    onStatusChange(notificationEvent)
  }

  companion object {
    private val LOG = Logger.getInstance(GradleExecutionAware::class.java)
  }
}
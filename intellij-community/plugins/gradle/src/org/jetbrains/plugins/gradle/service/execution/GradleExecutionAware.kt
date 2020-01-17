// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.execution

import com.intellij.build.events.impl.FinishEventImpl
import com.intellij.build.events.impl.ProgressBuildEventImpl
import com.intellij.build.events.impl.StartEventImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemBuildEvent
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemExecutionAware
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkException
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.notification.callback.OpenExternalSystemSettingsCallback
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker
import com.intellij.openapi.util.Disposer
import com.intellij.util.Consumer
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.GradleBundle.PATH_TO_BUNDLE
import java.lang.System.currentTimeMillis
import java.util.concurrent.CountDownLatch

class GradleExecutionAware : ExternalSystemExecutionAware {
  override fun prepareExecution(
    taskId: ExternalSystemTaskId,
    externalProjectPath: String,
    isPreviewMode: Boolean,
    taskNotificationListener: ExternalSystemTaskNotificationListener,
    project: Project
  ) {
    if (!isPreviewMode) prepareJvmForExecution(taskId, externalProjectPath, taskNotificationListener, project)
  }

  private fun prepareJvmForExecution(
    taskId: ExternalSystemTaskId,
    externalProjectPath: String,
    taskNotificationListener: ExternalSystemTaskNotificationListener,
    project: Project
  ) {
    val settings = GradleSettings.getInstance(project)
    val projectSettings = settings.getLinkedProjectSettings(externalProjectPath) ?: return
    val gradleJvm = projectSettings.gradleJvm ?: jdkConfigurationError("gradle.jvm.is.invalid")

    val sdk = try {
      ExternalSystemJdkUtil.getJdk(project, gradleJvm) ?: jdkConfigurationError("gradle.jvm.is.invalid")
    }
    catch (e: ExternalSystemJdkException) {
      jdkConfigurationError("gradle.jvm.is.invalid")
    }

    waitForDownloadingIfNeeded(sdk, taskId, taskNotificationListener, project)

    if (!ExternalSystemJdkUtil.isValidJdk(sdk)) {
      val sdkHomePath = sdk.homePath ?: jdkConfigurationError("gradle.jvm.is.invalid")
      if (JdkUtil.checkForJre(sdkHomePath)) {
        jdkConfigurationError("gradle.jvm.is.jre")
      }
      jdkConfigurationError("gradle.jvm.is.invalid")
    }
  }

  private fun jdkConfigurationError(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String, cause: Throwable? = null): Nothing {
    val errorMessage = GradleBundle.message(key)
    val openSettingsMessage = GradleBundle.message("gradle.open.gradle.settings")
    val exceptionMessage = String.format("$errorMessage <a href='%s'>$openSettingsMessage</a> \n", OpenExternalSystemSettingsCallback.ID)
    throw ExternalSystemJdkException(exceptionMessage, cause, OpenExternalSystemSettingsCallback.ID)
  }

  private fun waitForDownloadingIfNeeded(
    sdk: Sdk,
    taskId: ExternalSystemTaskId,
    taskNotificationListener: ExternalSystemTaskNotificationListener,
    project: Project
  ) {
    val downloadTracker = SdkDownloadTracker.getInstance()
    val progressIndicator = createProgressIndicator(taskId, taskNotificationListener)
    val countDownLatch = CountDownLatch(1)
    val disposable = Disposable { countDownLatch.countDown() }
    val isDownloadingInProgress = invokeAndWaitIfNeeded {
      downloadTracker.tryRegisterDownloadingListener(sdk, project, progressIndicator, Consumer {
        Disposer.dispose(disposable)
      })
    }
    if (!isDownloadingInProgress) return
    Disposer.register(project, disposable)

    if (ApplicationManager.getApplication().isDispatchThread) {
      LOG.error("Do not perform synchronous wait for sdk downloading in EDT - causes deadlock.")
      jdkConfigurationError("gradle.jvm.is.downloading")
    }

    taskNotificationListener.submitProgressStarted(taskId, progressIndicator)
    countDownLatch.await()
    taskNotificationListener.submitProgressFinished(taskId, progressIndicator)
  }

  private fun createProgressIndicator(taskId: ExternalSystemTaskId, taskNotificationListener: ExternalSystemTaskNotificationListener) =
    object : AbstractProgressIndicatorExBase() {
      override fun setFraction(fraction: Double) {
        super.setFraction(fraction)
        taskNotificationListener.submitProgressStatus(taskId, this)
      }
    }

  private fun ExternalSystemTaskNotificationListener.submitProgressStarted(
    taskId: ExternalSystemTaskId,
    progressIndicator: ProgressIndicator
  ) {
    val buildEvent = StartEventImpl(progressIndicator, taskId, currentTimeMillis(), progressIndicator.text)
    val notificationEvent = ExternalSystemBuildEvent(taskId, buildEvent)
    onStatusChange(notificationEvent)
  }

  private fun ExternalSystemTaskNotificationListener.submitProgressFinished(
    taskId: ExternalSystemTaskId,
    progressIndicator: ProgressIndicator
  ) {
    val result = SuccessResultImpl()
    val buildEvent = FinishEventImpl(progressIndicator, taskId, currentTimeMillis(), progressIndicator.text, result)
    val notificationEvent = ExternalSystemBuildEvent(taskId, buildEvent)
    onStatusChange(notificationEvent)
  }

  private fun ExternalSystemTaskNotificationListener.submitProgressStatus(
    taskId: ExternalSystemTaskId,
    progressIndicator: ProgressIndicator
  ) {
    val progress = (progressIndicator.fraction * 100).toLong()
    val buildEvent = ProgressBuildEventImpl(progressIndicator, taskId, currentTimeMillis(), progressIndicator.text, 100, progress, "%")
    val notificationEvent = ExternalSystemBuildEvent(taskId, buildEvent)
    onStatusChange(notificationEvent)
  }

  companion object {
    private val LOG = Logger.getInstance(GradleExecutionAware::class.java)
  }
}
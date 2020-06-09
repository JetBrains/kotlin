// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("GradleExecutionUtil")
package org.jetbrains.plugins.gradle.service.execution

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType.EXECUTE_TASK
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkProvider
import com.intellij.openapi.externalSystem.service.internal.AbstractExternalSystemTask
import com.intellij.openapi.externalSystem.service.remote.ExternalSystemProgressNotificationManagerImpl
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import org.gradle.tooling.GradleConnector
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.service.GradleInstallationManager
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.nio.file.Path

fun ensureInstalledWrapper(project: Project, externalProjectPath: Path, gradleVersion: GradleVersion, callback: Runnable) {
  val ensureInstalledWrapperTask = EnsureInstalledWrapperExecutionTask(project, externalProjectPath, gradleVersion)
  val title = GradleBundle.message("gradle.project.generation.wrapper.progress.title")
  val task = object : Task.Backgroundable(project, title, true) {
    override fun run(indicator: ProgressIndicator) {
      val listener = object : ExternalSystemTaskNotificationListenerAdapter() {
        override fun onEnd(id: ExternalSystemTaskId) = callback.run()
      }
      ensureInstalledWrapperTask.execute(indicator, listener)
    }
  }
  task.queue()
}

private class EnsureInstalledWrapperExecutionTask(
  project: Project,
  externalProjectPath: Path,
  private val gradleVersion: GradleVersion
) : AbstractExternalSystemTask(SYSTEM_ID, EXECUTE_TASK, project, externalProjectPath.toString()) {
  private val progressNotificationManager = ExternalSystemProgressNotificationManagerImpl.getInstanceImpl()
  private val newCancellationTokenSource = GradleConnector.newCancellationTokenSource()

  private fun createExecutionSettings(): GradleExecutionSettings {
    val settings = GradleSettings.getInstance(ideProject)
    val executionSettings = GradleExecutionSettings(
      getGradleHome(),
      settings.serviceDirectoryPath,
      getDistributionType(),
      settings.gradleVmOptions,
      settings.isOfflineWork
    )
    val jdkProvider = ExternalSystemJdkProvider.getInstance()
    executionSettings.javaHome = jdkProvider.internalJdk.homePath
    return executionSettings
  }

  private fun getGradleHome(): String? {
    val installationManager = GradleInstallationManager.getInstance()
    val gradleHome = installationManager.getGradleHome(ideProject, externalProjectPath)
    return FileUtil.toCanonicalPath(gradleHome?.path)
  }

  private fun getDistributionType(): DistributionType {
    val settings = GradleSettings.getInstance(ideProject)
    val projectSettings = settings.getLinkedProjectSettings(externalProjectPath)
    return when {
      projectSettings != null -> projectSettings.distributionType ?: DistributionType.LOCAL
      GradleUtil.isGradleDefaultWrapperFilesExist(externalProjectPath) -> DistributionType.DEFAULT_WRAPPED
      else -> DistributionType.BUNDLED
    }
  }

  private fun ensureInstalledWrapper(listener: ExternalSystemTaskNotificationListener) {
    GradleExecutionHelper().ensureInstalledWrapper(
      id,
      externalProjectPath,
      createExecutionSettings(),
      gradleVersion,
      listener,
      newCancellationTokenSource.token()
    )
  }

  override fun doExecute() {
    val progressNotificationListener = wrapWithListener(progressNotificationManager)
    try {
      progressNotificationManager.onStart(id, externalProjectPath)
      ensureInstalledWrapper(progressNotificationListener)
      progressNotificationManager.onSuccess(id)
    }
    catch (e: Exception) {
      progressNotificationManager.onFailure(id, e)
      throw e
    }
    finally {
      progressNotificationManager.onEnd(id)
    }
  }

  override fun doCancel(): Boolean {
    progressNotificationManager.beforeCancel(id)
    newCancellationTokenSource.cancel()
    progressNotificationManager.onCancel(id)
    return true
  }
}
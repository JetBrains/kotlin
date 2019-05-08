// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.issue.quickfix

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode.IN_BACKGROUND_ASYNC
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil.runTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.ApiStatus
import com.intellij.build.issue.BuildIssueQuickFix
import org.jetbrains.plugins.gradle.issue.quickfix.GradleWrapperSettingsOpenQuickFix.Companion.showWrapperPropertiesFile
import org.jetbrains.plugins.gradle.issue.quickfix.ReimportQuickFix.Companion.requestImport
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
class GradleVersionQuickFix(private val myProjectPath: String,
                            private val myGradleVersion: GradleVersion,
                            private val myRequestImport: Boolean) : BuildIssueQuickFix {

  override val id: String = "fix_gradle_version_in_wrapper"

  override fun runQuickFix(project: Project): CompletableFuture<*> {
    return runWrapperTask(project)
      .thenApply { isWrapperTaskSucceed ->
        if (!isWrapperTaskSucceed) return@thenApply false
        showWrapperPropertiesFile(project, myProjectPath, myGradleVersion.version)
        val projectSettings = GradleSettings.getInstance(project).getLinkedProjectSettings(myProjectPath) ?: return@thenApply false
        projectSettings.distributionType = DistributionType.DEFAULT_WRAPPED
        return@thenApply true
      }
      .thenCompose { isDistributionTypeSet ->
        if (myRequestImport && isDistributionTypeSet!!)
          requestImport(project, myProjectPath)
        else
          completedFuture(false)
      }
  }

  private fun runWrapperTask(project: Project): CompletableFuture<Boolean> {
    val userData = UserDataHolderBase()
    val initScript = "gradle.projectsEvaluated { g ->\n" +
                     "  def wrapper = g.rootProject.tasks.wrapper\n" +
                     "  if (wrapper == null) return \n" +
                     "  wrapper.gradleVersion = '" + myGradleVersion.version + "'\n" +
                     "}\n"
    userData.putUserData(GradleTaskManager.INIT_SCRIPT_KEY, initScript)
    // todo use Sync tab when multiple-build view will be integrated with the BuildTreeConsoleView
    //userData.putUserData(PROGRESS_LISTENER_KEY, SyncViewManager.class);

    val gradleVmOptions = GradleSettings.getInstance(project).gradleVmOptions
    val settings = ExternalSystemTaskExecutionSettings()
    settings.executionName = "Upgrade Gradle wrapper"
    settings.externalProjectPath = myProjectPath
    settings.taskNames = listOf("wrapper")
    settings.vmOptions = gradleVmOptions
    settings.externalSystemIdString = GradleConstants.SYSTEM_ID.id

    val future = CompletableFuture<Boolean>()
    runTask(settings, DefaultRunExecutor.EXECUTOR_ID, project, GradleConstants.SYSTEM_ID,
            object : TaskCallback {
              override fun onSuccess() {
                future.complete(true)
              }

              override fun onFailure() {
                future.complete(false)
              }
            }, IN_BACKGROUND_ASYNC, false, userData)
    return future
  }
}

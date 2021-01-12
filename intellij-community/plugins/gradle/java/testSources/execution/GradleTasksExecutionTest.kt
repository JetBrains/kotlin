// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.Test
import org.junit.runners.Parameterized
import java.util.concurrent.CompletableFuture

class GradleTasksExecutionTest : GradleImportingTestCase() {

  @Test
  fun `run task with specified build file test`() {
    createProjectSubFile("build.gradle", """
      task myTask() { doLast { print 'Hi!' } }
      """.trimIndent())
    createProjectSubFile("build007.gradle", """
      task anotherTask() { doLast { print 'Hi, James!' } }
      """.trimIndent())

    assertThat(runTaskAndGetErrorOutput(projectPath, "myTask")).isEmpty()
    assertThat(runTaskAndGetErrorOutput("$projectPath/build.gradle", "myTask")).isEmpty()
    assertThat(runTaskAndGetErrorOutput(projectPath, "anotherTask")).contains("Task 'anotherTask' not found in root project 'project'.")
    assertThat(runTaskAndGetErrorOutput("$projectPath/build007.gradle", "anotherTask")).isEmpty()
    assertThat(runTaskAndGetErrorOutput("$projectPath/build007.gradle", "myTask")).contains(
      "Task 'myTask' not found in root project 'project'.")

    assertThat(runTaskAndGetErrorOutput("$projectPath/build.gradle", "myTask", "-b foo")).contains("The specified build file",
                                                                                                   "foo' does not exist.")
  }

  private fun runTaskAndGetErrorOutput(projectPath: String, taskName: String, scriptParameters: String = ""): String {
    val taskErrOutput = StringBuilder()
    val stdErrListener = object : ExternalSystemTaskNotificationListenerAdapter() {
      override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
        if (!stdOut) {
          taskErrOutput.append(text)
        }
      }
    }
    val notificationManager = ExternalSystemProgressNotificationManager.getInstance()
    notificationManager.addNotificationListener(stdErrListener)
    try {
      val settings = ExternalSystemTaskExecutionSettings()
      settings.externalProjectPath = projectPath
      settings.taskNames = listOf(taskName)
      settings.scriptParameters = scriptParameters
      settings.externalSystemIdString = GradleConstants.SYSTEM_ID.id

      val future = CompletableFuture<String>()
      ExternalSystemUtil.runTask(settings, DefaultRunExecutor.EXECUTOR_ID, myProject, GradleConstants.SYSTEM_ID,
                                 object : TaskCallback {
                                   override fun onSuccess() {
                                     future.complete(taskErrOutput.toString())
                                   }

                                   override fun onFailure() {
                                     future.complete(taskErrOutput.toString())
                                   }
                                 }, ProgressExecutionMode.IN_BACKGROUND_ASYNC)
      return future.get()
    }
    finally {
      notificationManager.removeNotificationListener(stdErrListener)
    }
  }

  companion object {
    /**
     * It's sufficient to run the test against one gradle version
     */
    @Parameterized.Parameters(name = "with Gradle-{0}")
    @JvmStatic
    fun tests(): Collection<Array<out String>> = arrayListOf(arrayOf(BASE_GRADLE_VERSION))
  }
}
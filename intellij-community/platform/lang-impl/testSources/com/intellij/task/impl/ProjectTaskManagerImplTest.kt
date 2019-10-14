// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.task.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.task.*
import com.intellij.task.ProjectTaskManagerTest.Companion.doTestDeprecatedApi
import com.intellij.task.ProjectTaskManagerTest.Companion.doTestNewApi
import com.intellij.testFramework.ExtensionTestUtil.maskExtensions
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.ui.UIUtil
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.isPending
import org.jetbrains.concurrency.resolvedPromise
import org.junit.Test
import java.util.concurrent.TimeUnit

class ProjectTaskManagerImplTest : LightPlatformTestCase() {

  @Test
  fun `test new api calls`() {
    val tasks = getTasksToRun(testRootDisposable)
    doTestNewApi(ProjectTaskManagerImpl.getInstance(project), tasks, promiseHandler())
  }

  @Test
  fun `test deprecated api calls`() {
    val tasks = getTasksToRun(testRootDisposable)
    doTestDeprecatedApi(ProjectTaskManagerImpl.getInstance(project), tasks, promiseHandler())
  }
}

private fun <T> promiseHandler(): (Promise<T?>) -> T? = {
  val startTime = System.currentTimeMillis()
  while (it.isPending) {
    val durationInSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime)
    check(durationInSeconds < 5) { "Project task result was not received after $durationInSeconds sec" }
    UIUtil.dispatchAllInvocationEvents()
  }
  it.blockingGet(10)
}

private class DummyTask : ProjectTask {
  override fun getPresentableName(): String {
    return "dummy task"
  }
}

private fun getTasksToRun(testRootDisposable: Disposable): ProjectTaskList {
  val task1 = DummyTask()
  val runner1 = object : ProjectTaskRunner() {
    override fun canRun(projectTask: ProjectTask) = projectTask == task1
    override fun run(project: Project, context: ProjectTaskContext, vararg tasks: ProjectTask?): Promise<Result> {
      return resolvedPromise(TaskRunnerResults.SUCCESS)
    }
  }

  val task2 = DummyTask()
  val runner2 = object : ProjectTaskRunner() {
    override fun canRun(projectTask: ProjectTask) = projectTask == task2
    override fun run(project: Project, context: ProjectTaskContext, callback: ProjectTaskNotification?, vararg tasks: ProjectTask?) {
      callback?.finished(ProjectTaskResult(false, 0, 0))
    }
  }
  val task3 = DummyTask()
  val runner3 = object : ProjectTaskRunner() {
    override fun canRun(projectTask: ProjectTask) = projectTask == task3
    override fun run(project: Project,
                     context: ProjectTaskContext,
                     callback: ProjectTaskNotification?,
                     tasks: Collection<ProjectTask>) {
      callback?.finished(ProjectTaskResult(false, 0, 0))
    }
  }
  val task4 = DummyTask()
  val runner4 = object : ProjectTaskRunner() {
    override fun canRun(projectTask: ProjectTask) = projectTask == task4
    override fun run(project: Project,
                     context: ProjectTaskContext,
                     callback: ProjectTaskNotification?,
                     tasks: Collection<ProjectTask>) {
      callback?.finished(context, ProjectTaskResult(false, 0, 0))
    }
  }
  maskExtensions(ProjectTaskRunner.EP_NAME, listOf(runner1, runner2, runner3, runner4), testRootDisposable)
  return ProjectTaskList.asList(task1, task2, task3, task4, DummyTask())
}
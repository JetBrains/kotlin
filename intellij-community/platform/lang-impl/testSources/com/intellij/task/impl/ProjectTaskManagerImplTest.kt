// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("DEPRECATION")

package com.intellij.task.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectModelBuildableElement
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.task.*
import com.intellij.testFramework.ExtensionTestUtil.maskExtensions
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.ui.UIUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.isPending
import org.jetbrains.concurrency.resolvedPromise
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.TimeUnit

class ProjectTaskManagerImplTest : LightPlatformTestCase() {

  @Test
  fun `test new api calls`() {
    val tasks = getTasksToRun(testRootDisposable)
    val taskManager = ProjectTaskManagerImpl.getInstance(project)
    val promiseHandler: (Promise<ProjectTaskManager.Result?>) -> ProjectTaskManager.Result? = promiseHandler()
    val context = ProjectTaskContext()
    Assert.assertNotNull(taskManager.run(tasks).run(promiseHandler))
    Assert.assertEquals(context, taskManager.run(context, tasks).run(promiseHandler)!!.context)
    Assert.assertNotNull(taskManager.buildAllModules().run(promiseHandler))
    Assert.assertNotNull(taskManager.rebuildAllModules().run(promiseHandler))
    Assert.assertNotNull(taskManager.build(*Module.EMPTY_ARRAY).run(promiseHandler))
    Assert.assertNotNull(taskManager.rebuild(*Module.EMPTY_ARRAY).run(promiseHandler))
    Assert.assertNotNull(taskManager.compile(*VirtualFile.EMPTY_ARRAY).run(promiseHandler))
    val emptyArray = emptyArray<ProjectModelBuildableElement>()
    Assert.assertNotNull(taskManager.build(*emptyArray).run(promiseHandler))
    Assert.assertNotNull(taskManager.rebuild(*emptyArray).run(promiseHandler))
  }

  @Test
  fun `test deprecated api calls`() {
    val tasks = getTasksToRun(testRootDisposable)
    val taskManager = ProjectTaskManagerImpl.getInstance(project)
    val promiseHandler: (Promise<ProjectTaskResult?>) -> ProjectTaskResult? = promiseHandler()
    fun doTest(body: (ProjectTaskNotification) -> Unit) {
      val promise1 = AsyncPromise<ProjectTaskResult?>()
      body.invoke(object : ProjectTaskNotification {
        override fun finished(executionResult: ProjectTaskResult) {
          promise1.setResult(executionResult)
        }
      })
      Assert.assertNotNull(promise1.run(promiseHandler))

      val promise2 = AsyncPromise<ProjectTaskResult?>()
      body.invoke(object : ProjectTaskNotification {
        override fun finished(context: ProjectTaskContext, executionResult: ProjectTaskResult) {
          promise2.setResult(executionResult)
        }
      })
      Assert.assertNotNull(promise2.run(promiseHandler))
    }

    val context = ProjectTaskContext()
    doTest { taskManager.run(tasks, it) }
    doTest { taskManager.run(context, tasks, it) }
    doTest { taskManager.buildAllModules(it) }
    doTest { taskManager.rebuildAllModules(it) }
    doTest { taskManager.build(Module.EMPTY_ARRAY, it) }
    doTest { taskManager.rebuild(Module.EMPTY_ARRAY, it) }
    doTest { taskManager.compile(VirtualFile.EMPTY_ARRAY, it) }
    doTest { taskManager.build(arrayOf<ProjectModelBuildableElement>(), it) }
    doTest { taskManager.rebuild(arrayOf<ProjectModelBuildableElement>(), it) }
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
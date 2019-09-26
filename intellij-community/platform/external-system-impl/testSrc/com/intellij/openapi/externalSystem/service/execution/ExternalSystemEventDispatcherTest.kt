// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution

import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.impl.*
import com.intellij.build.output.BuildOutputParser
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.concurrency.Semaphore

class ExternalSystemEventDispatcherTest : LightPlatformTestCase() {

  fun testInvokeOnCompletion() {
    val testSystemId = ProjectSystemId("ExternalSystemEventDispatcherTest")
    val taskId = ExternalSystemTaskId.create(testSystemId, ExternalSystemTaskType.EXECUTE_TASK, "test")

    val eventMessages = mutableListOf<String>()
    val progressListener = BuildProgressListener { _, event -> eventMessages += event.message }

    ExtensionTestUtil.maskExtensions(ExternalSystemOutputParserProvider.EP_NAME,
                                     listOf(object : ExternalSystemOutputParserProvider {
                                       override fun getExternalSystemId(): ProjectSystemId = testSystemId

                                       override fun getBuildOutputParsers(taskId: ExternalSystemTaskId): MutableList<BuildOutputParser> {
                                         return mutableListOf(BuildOutputParser { line, reader, messageConsumer ->
                                           messageConsumer.accept(OutputBuildEventImpl(reader.parentEventId, line, true))
                                           return@BuildOutputParser true
                                         })
                                       }
                                     }),
                                     testRootDisposable)
    val semaphore = Semaphore(1)
    val dispatcher = ExternalSystemEventDispatcher(taskId, progressListener)
    dispatcher.use {
      it.onEvent(1, StartBuildEventImpl(DefaultBuildDescriptor(taskId, "test task", "/path", System.currentTimeMillis()), "Build started"))
      it.invokeOnCompletion { eventMessages += "completion message 1" }
      it.onEvent(1, StartEventImpl(2, 1, System.currentTimeMillis(), "sub task1 started"))
      it.appendln("Task output line 1")
      it.onEvent(1, StartEventImpl(3, 1, System.currentTimeMillis(), "sub task2 started"))
      it.appendln("Task output line 2")
      it.onEvent(1, FinishEventImpl(3, 1, System.currentTimeMillis(), "sub task2 finished", SuccessResultImpl()))
      it.onEvent(1, FinishEventImpl(2, 1, System.currentTimeMillis(), "sub task1 finished", SuccessResultImpl()))
      it.onEvent(1, FinishBuildEventImpl(taskId, null, System.currentTimeMillis(), "Build finished", SuccessResultImpl()))
      it.invokeOnCompletion { throw RuntimeException("Bad happens") }
      it.invokeOnCompletion { eventMessages += "completion message 2" }
      it.invokeOnCompletion { semaphore.up() }
      it.appendln("Task output line 3")
    }

    assertTrue(semaphore.waitFor(500))

    assertSameElements(eventMessages.take(8),
                       "Build started",
                       "sub task1 started",
                       "sub task2 started",
                       "sub task2 finished",
                       "sub task1 finished",
                       "Task output line 1",
                       "Task output line 2",
                       "Task output line 3")

    assertOrderedEquals(eventMessages.takeLast(3),
                        "completion message 1",
                        "Build finished",
                        "completion message 2")
  }
}

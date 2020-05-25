// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.remote

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.internal.AbstractExternalSystemTask
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.externalSystem.service.remote.ExternalSystemProgressNotificationManagerImpl.Companion.assertListenersReleased
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.util.ThrowableRunnable

class ExternalSystemProgressNotificationManagerImplTest : UsefulTestCase() {

  lateinit var testFixture: IdeaProjectTestFixture
  lateinit var project: Project

  override fun setUp() {
    super.setUp()

    testFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(name).fixture
    testFixture.setUp()
    project = testFixture.project
  }

  override fun tearDown() {
    RunAll(
      ThrowableRunnable { testFixture.tearDown() },
      ThrowableRunnable { super.tearDown() }
    ).run()
  }

  fun `test listener cleanup`() {
    val disposable = Disposer.newDisposable(testRootDisposable, "test task listener cleanup")
    ExternalSystemProgressNotificationManager.getInstance().addNotificationListener(DummyTaskNotificationListener(), disposable)
    val task = MyTestTask(project) { it.cancel(DummyTaskNotificationListener()) }
    ExternalSystemProgressNotificationManager.getInstance().addNotificationListener(task.id, DummyTaskNotificationListener())
    task.execute(DummyTaskNotificationListener())
    task.cancel(DummyTaskNotificationListener())
    assertListenersReleased(task.id)

    Disposer.dispose(disposable)
    assertListenersReleased()
  }

  private class DummyTaskNotificationListener : ExternalSystemTaskNotificationListenerAdapter()
  private class MyTestTask(project: Project, private val actionBeforeTaskFinish: (MyTestTask) -> Unit = {}) :
    AbstractExternalSystemTask(ProjectSystemId.IDE, ExternalSystemTaskType.EXECUTE_TASK, project, "") {
    override fun doCancel(): Boolean = true

    override fun doExecute() {
      ExternalSystemProgressNotificationManagerImpl.getInstanceImpl().run {
        onStart(id, "")
        actionBeforeTaskFinish.invoke(this@MyTestTask)
        onEnd(id)
      }
    }
  }
}
// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.externalSystem.ExternalSystemManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager
import com.intellij.openapi.externalSystem.test.ExternalSystemTestUtil.TEST_EXTERNAL_SYSTEM_ID
import com.intellij.openapi.externalSystem.test.TestExternalSystemExecutionSettings
import com.intellij.openapi.externalSystem.test.TestExternalSystemManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.util.ThrowableRunnable
import junit.framework.TestCase

class ExternalSystemFacadeManagerTest : UsefulTestCase() {

  lateinit var testFixture: IdeaProjectTestFixture
  lateinit var project: Project

  override fun setUp() {
    super.setUp()

    testFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(name).fixture
    testFixture.setUp()
    project = testFixture.project

    val testManager = object: TestExternalSystemManager(project) {
      override fun getProjectResolverClass(): Class<out ExternalSystemProjectResolver<TestExternalSystemExecutionSettings>> {
        return TestProjectResolver::class.java
      }

      override fun getTaskManagerClass(): Class<out ExternalSystemTaskManager<TestExternalSystemExecutionSettings>> {
        return TestTaskManager::class.java
      }
    }

    PlatformTestUtil.maskExtensions(ExternalSystemManager.EP_NAME, listOf(testManager), testRootDisposable)
  }

  override fun tearDown() {
    RunAll(
      ThrowableRunnable { testFixture.tearDown() },
      ThrowableRunnable { super.tearDown() }
    ).run()
  }

  fun `test remote resolve project info`() {
    val facadeManager: ExternalSystemFacadeManager = ServiceManager.getService(ExternalSystemFacadeManager::class.java)
    TestCase.assertNotNull(facadeManager)
    val facade = facadeManager.getFacade(project, "fake/path", TEST_EXTERNAL_SYSTEM_ID)
    try {
      TestCase.assertNotNull(facade)

      val taskId = ExternalSystemTaskId.create(TEST_EXTERNAL_SYSTEM_ID, ExternalSystemTaskType.RESOLVE_PROJECT, project)
      val projectDataNode = facade.resolver.resolveProjectInfo(taskId, "fake/path", false, null);

      assertNotNull(projectDataNode)
      assertEquals("ExternalName", projectDataNode!!.data.externalName)

    } finally {
      facadeManager.getCommunicationManager(TEST_EXTERNAL_SYSTEM_ID).release("fake/path", TEST_EXTERNAL_SYSTEM_ID)
    }
  }
}


class TestProjectResolver: ExternalSystemProjectResolver<TestExternalSystemExecutionSettings> {
  override fun resolveProjectInfo(id: ExternalSystemTaskId,
                                  projectPath: String,
                                  isPreviewMode: Boolean,
                                  settings: TestExternalSystemExecutionSettings?,
                                  listener: ExternalSystemTaskNotificationListener): DataNode<ProjectData>? {
    val data = ProjectData(TEST_EXTERNAL_SYSTEM_ID, "ExternalName", "fake/path", "linked/project/path")
    return DataNode(ProjectKeys.PROJECT, data, null)
  }

  override fun cancelTask(taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
    listener.beforeCancel(taskId)
    listener.onCancel(taskId)
    return true
  }
}

class TestTaskManager: ExternalSystemTaskManager<TestExternalSystemExecutionSettings> {
  override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
    listener.beforeCancel(id)
    listener.onCancel(id)
    return true
  }

}
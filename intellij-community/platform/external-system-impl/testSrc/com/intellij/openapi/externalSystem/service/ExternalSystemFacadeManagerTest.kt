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
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.util.ThrowableRunnable
import junit.framework.TestCase
import java.net.URL
import java.net.URLClassLoader
import com.intellij.openapi.externalSystem.model.Key as DataNodeKey

class ExternalSystemFacadeManagerTest : UsefulTestCase() {

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

  fun `test remote resolve project info`() {
    ExtensionTestUtil.maskExtensions(ExternalSystemManager.EP_NAME, listOf(SimpleTestExternalSystemManager(project)), testRootDisposable)

    val facadeManager: ExternalSystemFacadeManager = ServiceManager.getService(ExternalSystemFacadeManager::class.java)
    TestCase.assertNotNull(facadeManager)
    val facade = facadeManager.getFacade(project, "fake/path", TEST_EXTERNAL_SYSTEM_ID)
    try {
      TestCase.assertNotNull(facade)

      val taskId = ExternalSystemTaskId.create(TEST_EXTERNAL_SYSTEM_ID, ExternalSystemTaskType.RESOLVE_PROJECT, project)
      val projectDataNode = facade.resolver.resolveProjectInfo(taskId, "fake/path", false, null, null);

      assertNotNull(projectDataNode)
      assertEquals("ExternalName", projectDataNode!!.data.externalName)

    } finally {
      facadeManager.getCommunicationManager(TEST_EXTERNAL_SYSTEM_ID).release("fake/path", TEST_EXTERNAL_SYSTEM_ID)
    }
  }

  fun `test remote resolve with custom classes`() {
    val libUrl = javaClass.classLoader.getResource("dataNodeTest/lib.jar")!!

    val subClassLoader = URLClassLoader(arrayOf(libUrl), this::class.java.classLoader)


    val fakeExternalSystemManager = subClassLoader
      .loadClass("com.intellij.openapi.externalSystem.service.FakeExternalSystemManager")
      .getConstructor(Project::class.java)
      .newInstance(project) as ExternalSystemManager<*, *, *, *, *>

    val testExternalSystemManager = CustomClassLoadingTestExternalSystemManager(project)

    ExtensionTestUtil.maskExtensions(ExternalSystemManager.EP_NAME,
                                    listOf(testExternalSystemManager, fakeExternalSystemManager),
                                    testRootDisposable)

    val facadeManager: ExternalSystemFacadeManager = ServiceManager.getService(ExternalSystemFacadeManager::class.java)
    TestCase.assertNotNull(facadeManager)
    val facade = facadeManager.getFacade(project, "fake/path", TEST_EXTERNAL_SYSTEM_ID)
    try {
      val taskId = ExternalSystemTaskId.create(TEST_EXTERNAL_SYSTEM_ID, ExternalSystemTaskType.RESOLVE_PROJECT, project)
      val settings = CustomLibUrlSettings(libUrl)
      val projectDataNode = facade.resolver.resolveProjectInfo(taskId, "fake/path", false, settings, null);

      assertNotNull(projectDataNode)
      assertEquals("ExternalName", projectDataNode!!.data.externalName)

      assertEquals("foo.Bar", projectDataNode.children.first().data::class.java.name)

    } finally {
      facadeManager.getCommunicationManager(TEST_EXTERNAL_SYSTEM_ID).release("fake/path", TEST_EXTERNAL_SYSTEM_ID)
    }
  }
}

class SimpleTestExternalSystemManager(val project: Project): TestExternalSystemManager(project) {
  override fun getProjectResolverClass(): Class<out ExternalSystemProjectResolver<TestExternalSystemExecutionSettings>> {
    return TestProjectResolver::class.java
  }

  override fun getTaskManagerClass(): Class<out ExternalSystemTaskManager<TestExternalSystemExecutionSettings>> {
    return TestTaskManager::class.java
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


class CustomClassLoadingTestExternalSystemManager(val project: Project): TestExternalSystemManager(project) {
  override fun getProjectResolverClass(): Class<out ExternalSystemProjectResolver<TestExternalSystemExecutionSettings>> {
    return CustomClassLoadingTestProjectResolver::class.java
  }

  override fun getTaskManagerClass(): Class<out ExternalSystemTaskManager<TestExternalSystemExecutionSettings>> {
    return TestTaskManager::class.java
  }
}

class CustomClassLoadingTestProjectResolver: ExternalSystemProjectResolver<TestExternalSystemExecutionSettings> {

  companion object {
    val BarKey: DataNodeKey<Any> = DataNodeKey("foo.Bar", 0)
  }

  override fun resolveProjectInfo(id: ExternalSystemTaskId,
                                  projectPath: String,
                                  isPreviewMode: Boolean,
                                  settings: TestExternalSystemExecutionSettings?,
                                  listener: ExternalSystemTaskNotificationListener): DataNode<ProjectData>? {
    val libUrl = (settings as? CustomLibUrlSettings)?.libUrl
    val data = ProjectData(TEST_EXTERNAL_SYSTEM_ID, "ExternalName", "fake/path", "linked/project/path")
    val rootNode = DataNode(ProjectKeys.PROJECT, data, null)
    val cl = URLClassLoader(arrayOf(libUrl))
    val barInstance = cl.loadClass("foo.Bar").newInstance()
    val childNode = DataNode(BarKey, barInstance, null)
    rootNode.addChild(childNode)
    return rootNode
  }

  override fun cancelTask(taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
    listener.beforeCancel(taskId)
    listener.onCancel(taskId)
    return true
  }
}

class CustomLibUrlSettings(val libUrl: URL) : TestExternalSystemExecutionSettings()
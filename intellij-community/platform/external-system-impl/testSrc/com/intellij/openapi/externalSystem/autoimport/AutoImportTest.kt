// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import org.junit.Test

class AutoImportTest : AutoImportTestCase() {
  @Test
  fun `test simple modification tracking`() {
    val systemId = ProjectSystemId("External System")
    val projectId = ExternalSystemProjectId(systemId, projectPath)
    val projectAware = MockProjectAware(projectId)

    val settingsFile = createFile("settings.groovy")
    assertSimpleState(projectAware, refresh = 0, notified = false, event = "create unregistered settings")

    projectAware.settingsFiles.add(settingsFile.path)

    projectTracker.register(projectAware)
    assertSimpleState(projectAware, refresh = 1, notified = false, event = "register project without cache")

    settingsFile.insertString(0, "println 'hello'")
    assertSimpleState(projectAware, refresh = 1, notified = true, event = "modification of registered settings")
    projectTracker.scheduleProjectRefresh()
    assertSimpleState(projectAware, refresh = 2, notified = false, event = "project refresh")

    settingsFile.replaceString(9, 14, "hi")
    assertSimpleState(projectAware, refresh = 2, notified = true, event = "modification of registered settings")
    settingsFile.replaceString(9, 11, "hello")
    assertSimpleState(projectAware, refresh = 2, notified = false, event = "revert changes in registered settings")

    settingsFile.insertString(15, " ")
    assertSimpleState(projectAware, refresh = 2, notified = false, event = "empty modification")
    settingsFile.insertString(16, "//It is comment")
    assertSimpleState(projectAware, refresh = 2, notified = false, event = "comment modification")
  }

  @Test
  fun `test deletion tracking`() {
    val systemId = ProjectSystemId("External System")
    val projectId = ExternalSystemProjectId(systemId, projectPath)
    val projectAware = MockProjectAware(projectId)

    var settingsFile = createFile("settings.groovy", "println 'hello'")
    assertSimpleState(projectAware, refresh = 0, notified = false, event = "create unregistered settings")

    projectAware.settingsFiles.add(settingsFile.path)

    projectTracker.register(projectAware)
    assertSimpleState(projectAware, refresh = 1, notified = false, event = "register project without cache")

    settingsFile.delete()
    assertSimpleState(projectAware, refresh = 1, notified = true, event = "delete registered settings")
    projectTracker.scheduleProjectRefresh()
    assertSimpleState(projectAware, refresh = 2, notified = false, event = "project refresh")

    settingsFile = createFile("settings.groovy", "println 'hello'")
    assertSimpleState(projectAware, refresh = 2, notified = true, event = "create registered settings")
    projectTracker.scheduleProjectRefresh()
    assertSimpleState(projectAware, refresh = 3, notified = false, event = "project refresh")

    settingsFile.delete()
    assertSimpleState(projectAware, refresh = 3, notified = true, event = "delete registered settings")
    createFile("settings.groovy", "println 'hello'")
    assertSimpleState(projectAware, refresh = 3, notified = false, event = "create registered settings immediately after deleting")
  }

  @Test
  fun `test modification tracking with several settings files`() {
    val systemId = ProjectSystemId("External System")
    val projectId = ExternalSystemProjectId(systemId, projectPath)
    val projectAware = MockProjectAware(projectId)

    val settingsFile = createFile("settings.groovy", "println 'hello'")
    assertSimpleState(projectAware, refresh = 0, notified = false, event = "create unregistered settings")

    projectAware.settingsFiles.add(settingsFile.path)

    projectTracker.register(projectAware)
    assertSimpleState(projectAware, refresh = 1, notified = false, event = "register project without cache")

    val scriptFile = createFile("script.groovy", "println('hello')")
    assertSimpleState(projectAware, refresh = 1, notified = false, event = "create unregistered settings")
    projectAware.settingsFiles.add(scriptFile.path)
    settingsFile.replaceString(9, 14, "hi")
    assertSimpleState(projectAware, refresh = 1, notified = true, event = "modification of registered settings")
    settingsFile.replaceString(9, 11, "hello")
    assertSimpleState(projectAware, refresh = 1, notified = true, event = "try to revert changes if has other modification")
    projectTracker.scheduleProjectRefresh()
    assertSimpleState(projectAware, refresh = 2, notified = false, event = "project refresh")

    settingsFile.replaceString(9, 14, "hi")
    assertSimpleState(projectAware, refresh = 2, notified = true, event = "modification of registered settings")
    scriptFile.replaceString(9, 14, "hi")
    assertSimpleState(projectAware, refresh = 2, notified = true, event = "modification of registered settings")
    settingsFile.replaceString(9, 11, "hello")
    assertSimpleState(projectAware, refresh = 2, notified = true, event = "try to revert changes if has other modification")
    scriptFile.replaceString(9, 11, "hello")
    assertSimpleState(projectAware, refresh = 2, notified = false, event = "revert changes in registered settings")
  }

  @Test
  fun `test modification tracking with several sub projects`() {
    val systemId1 = ProjectSystemId("External System 1")
    val systemId2 = ProjectSystemId("External System 2")
    val projectId1 = ExternalSystemProjectId(systemId1, projectPath)
    val projectId2 = ExternalSystemProjectId(systemId2, projectPath)
    val projectAware1 = MockProjectAware(projectId1)
    val projectAware2 = MockProjectAware(projectId2)

    val scriptFile1 = createFile("script1.groovy")
    val scriptFile2 = createFile("script2.groovy")

    projectAware1.settingsFiles.add(scriptFile1.path)
    projectAware2.settingsFiles.add(scriptFile2.path)

    projectTracker.register(projectAware1)
    projectTracker.register(projectAware2)

    assertProjectAware(projectAware1, refresh = 1, event = "register project without cache")
    assertProjectAware(projectAware2, refresh = 1, event = "register project without cache")
    assertNotificationAware(notified = false, event = "register project without cache")

    scriptFile1.insertString(0, "println 1")
    assertProjectAware(projectAware1, refresh = 1, event = "modification of first settings")
    assertProjectAware(projectAware2, refresh = 1, event = "modification of first settings")
    assertNotificationAware(projectId1, notified = true, event = "modification of first settings")

    scriptFile2.insertString(0, "println 2")
    assertProjectAware(projectAware1, refresh = 1, event = "modification of second settings")
    assertProjectAware(projectAware2, refresh = 1, event = "modification of second settings")
    assertNotificationAware(projectId1, projectId2, notified = true, event = "modification of second settings")

    scriptFile1.replaceString(0, 9, "")
    assertProjectAware(projectAware1, refresh = 1, event = "revert changes at second settings")
    assertProjectAware(projectAware2, refresh = 1, event = "revert changes at second settings")
    assertNotificationAware(projectId2, notified = true, event = "revert changes at second settings")

    projectTracker.scheduleProjectRefresh()
    assertProjectAware(projectAware1, refresh = 1, event = "project refresh")
    assertProjectAware(projectAware2, refresh = 2, event = "project refresh")
    assertNotificationAware(notified = false, event = "project refresh")
  }

  @Test
  fun `test project link-unlink`() {
    val systemId = ProjectSystemId("External System")
    val projectId = ExternalSystemProjectId(systemId, projectPath)
    val projectAware = MockProjectAware(projectId)

    val settingsFile = createFile("settings.groovy")
    assertSimpleState(projectAware, refresh = 0, notified = false, event = "create unregistered settings")

    projectAware.settingsFiles.add(settingsFile.path)

    projectTracker.register(projectAware)
    assertSimpleState(projectAware, refresh = 1, subscribe = 1, unsubscribe = 0, notified = false, event = "register project without cache")

    settingsFile.insertString(0, "println 'hello'")
    assertSimpleState(projectAware, refresh = 1, notified = true, event = "modification of registered settings")

    projectTracker.remove(projectId)
    assertSimpleState(projectAware, refresh = 1, subscribe = 1, unsubscribe = 1, notified = false, event = "remove project")

    projectTracker.register(projectAware)
    assertSimpleState(projectAware, refresh = 2, subscribe = 2, unsubscribe = 1, notified = false, event = "register project without cache")
  }

  @Test
  fun `test external modification tracking`() {
    val systemId = ProjectSystemId("External System")
    val projectId = ExternalSystemProjectId(systemId, projectPath)
    val projectAware = MockProjectAware(projectId)

    val settingsFile = createFile("settings.groovy")
    val scriptFile = createFile("script.groovy")
    assertSimpleState(projectAware, refresh = 0, notified = false, event = "create unregistered settings")

    projectAware.settingsFiles.add(settingsFile.path)
    projectAware.settingsFiles.add(scriptFile.path)

    projectTracker.register(projectAware)
    assertSimpleState(projectAware, refresh = 1, notified = false, event = "register project without cache")

    settingsFile.insertString(0, "println 'hello'")
    assertSimpleState(projectAware, refresh = 1, notified = true, event = "modification of registered settings")

    modification("modification") {
      assertSimpleState(projectAware, refresh = 1, notified = false, event = "start external modification")
      settingsFile.replaceString(9, 14, "hi")
      assertSimpleState(projectAware, refresh = 1, notified = false, event = "external modification of registered settings")
    }
    assertSimpleState(projectAware, refresh = 2, notified = false, event = "complete external modification")
  }

  @Test
  fun `test complex external modification tracking`() {
    val systemId = ProjectSystemId("External System")
    val projectId = ExternalSystemProjectId(systemId, projectPath)
    val projectAware = MockProjectAware(projectId)

    val settingsFile = createFile("settings.groovy")
    val scriptFile = createFile("script.groovy")
    assertSimpleState(projectAware, refresh = 0, notified = false, event = "create unregistered settings")

    projectAware.settingsFiles.add(settingsFile.path)
    projectAware.settingsFiles.add(scriptFile.path)

    projectTracker.register(projectAware)
    assertSimpleState(projectAware, refresh = 1, notified = false, event = "register project without cache")

    modification("modification 1") {
      assertSimpleState(projectAware, refresh = 1, notified = false, event = "start first external modification")
      settingsFile.insertString(0, "println 'hello'")
      assertSimpleState(projectAware, refresh = 1, notified = false, event = "first external modification of registered settings")
      modification("modification 2") {
        assertSimpleState(projectAware, refresh = 1, notified = false, event = "start second external modification")
        settingsFile.replaceString(9, 14, "hi")
        assertSimpleState(projectAware, refresh = 1, notified = false, event = "second external modification of registered settings")
      }
      assertSimpleState(projectAware, refresh = 1, notified = false, event = "complete second external modification")
    }
    assertSimpleState(projectAware, refresh = 2, notified = false, event = "complete first external modification")
  }
}
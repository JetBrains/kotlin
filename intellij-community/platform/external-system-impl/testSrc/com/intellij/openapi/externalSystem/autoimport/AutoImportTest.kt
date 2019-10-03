// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.junit.Test
import java.io.File

class AutoImportTest : AutoImportTestCase() {
  @Test
  fun `test simple modification tracking`() {
    simpleTest("settings.groovy") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      settingsFile.appendString("println 'hello'")
      assertState(refresh = 1, notified = true, event = "modification of registered settings")
      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")

      settingsFile.replaceString("hello", "hi")
      assertState(refresh = 2, notified = true, event = "modification of registered settings")
      settingsFile.replaceString("hi", "hello")
      assertState(refresh = 2, notified = false, event = "revert changes in registered settings")

      settingsFile.appendString(" ")
      assertState(refresh = 2, notified = false, event = "empty modification")
      settingsFile.appendString("//It is comment")
      assertState(refresh = 2, notified = false, event = "comment modification")

      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")

      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")
    }
  }

  @Test
  fun `test unrecognized settings file`() {
    simpleTest("settings.elvish") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      settingsFile.appendString("q71Gpj5 .9jR°`N.")
      assertState(refresh = 1, notified = true, event = "modification of registered settings")
      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")

      settingsFile.replaceString("9jR°`N", "9`B")
      assertState(refresh = 2, notified = true, event = "modification of registered settings")
      settingsFile.replaceString("9`B", "9jR°`N")
      assertState(refresh = 2, notified = true, event = "try to revert changes in unrecognized settings")
      refreshProject()
      assertState(refresh = 3, notified = false, event = "project refresh")

      settingsFile.appendString(" ")
      assertState(refresh = 3, notified = true, event = "unrecognized empty modification")
      refreshProject()
      assertState(refresh = 4, notified = false, event = "project refresh")
      settingsFile.appendString("//1G iT zt^P1Fp")
      assertState(refresh = 4, notified = true, event = "unrecognized comment modification")
    }
  }

  @Test
  fun `test deletion tracking`() {
    simpleTest("settings.groovy", "println 'hello'") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      settingsFile.delete()
      assertState(refresh = 1, notified = true, event = "delete registered settings")
      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")

      val newSettingsFile = findOrCreateFile("settings.groovy", "println 'hello'")
      assertState(refresh = 2, notified = true, event = "create registered settings")
      refreshProject()
      assertState(refresh = 3, notified = false, event = "project refresh")

      newSettingsFile.delete()
      assertState(refresh = 3, notified = true, event = "delete registered settings")
      findOrCreateFile("settings.groovy", "println 'hello'")
      assertState(refresh = 3, notified = false, event = "create registered settings immediately after deleting")
    }
  }

  @Test
  fun `test modification tracking with several settings files`() {
    simpleTest("settings.groovy", "println 'hello'") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      val scriptFile = findOrCreateFile("script.groovy", "println('hello')")
      assertState(refresh = 1, notified = false, event = "create unregistered settings")
      registerSettingsFile(scriptFile)
      settingsFile.replaceString("hello", "hi")
      assertState(refresh = 1, notified = true, event = "modification of registered settings")
      settingsFile.replaceString("hi", "hello")
      assertState(refresh = 1, notified = true, event = "try to revert changes if has other modification")
      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")

      settingsFile.replaceString("hello", "hi")
      assertState(refresh = 2, notified = true, event = "modification of registered settings")
      scriptFile.replaceString("hello", "hi")
      assertState(refresh = 2, notified = true, event = "modification of registered settings")
      settingsFile.replaceString("hi", "hello")
      assertState(refresh = 2, notified = true, event = "try to revert changes if has other modification")
      scriptFile.replaceString("hi", "hello")
      assertState(refresh = 2, notified = false, event = "revert changes in registered settings")
    }
  }

  @Test
  fun `test modification tracking with several sub projects`() {
    val systemId1 = ProjectSystemId("External System 1")
    val systemId2 = ProjectSystemId("External System 2")
    val projectId1 = ExternalSystemProjectId(systemId1, projectPath)
    val projectId2 = ExternalSystemProjectId(systemId2, projectPath)
    val projectAware1 = MockProjectAware(projectId1)
    val projectAware2 = MockProjectAware(projectId2)

    loadState(ProjectTracker.State())

    val scriptFile1 = findOrCreateFile("script1.groovy")
    val scriptFile2 = findOrCreateFile("script2.groovy")

    projectAware1.settingsFiles.add(scriptFile1.path)
    projectAware2.settingsFiles.add(scriptFile2.path)

    register(projectAware1)
    register(projectAware2)

    assertProjectAware(projectAware1, refresh = 1, event = "register project without cache")
    assertProjectAware(projectAware2, refresh = 1, event = "register project without cache")
    assertNotificationAware(notified = false, event = "register project without cache")

    scriptFile1.appendString("println 1")
    assertProjectAware(projectAware1, refresh = 1, event = "modification of first settings")
    assertProjectAware(projectAware2, refresh = 1, event = "modification of first settings")
    assertNotificationAware(projectId1, notified = true, event = "modification of first settings")

    scriptFile2.appendString("println 2")
    assertProjectAware(projectAware1, refresh = 1, event = "modification of second settings")
    assertProjectAware(projectAware2, refresh = 1, event = "modification of second settings")
    assertNotificationAware(projectId1, projectId2, notified = true, event = "modification of second settings")

    scriptFile1.removeContent()
    assertProjectAware(projectAware1, refresh = 1, event = "revert changes at second settings")
    assertProjectAware(projectAware2, refresh = 1, event = "revert changes at second settings")
    assertNotificationAware(projectId2, notified = true, event = "revert changes at second settings")

    refreshProject()
    assertProjectAware(projectAware1, refresh = 1, event = "project refresh")
    assertProjectAware(projectAware2, refresh = 2, event = "project refresh")
    assertNotificationAware(notified = false, event = "project refresh")

    scriptFile1.replaceContent("println 'script 1'")
    scriptFile2.replaceContent("println 'script 2'")
    assertProjectAware(projectAware1, refresh = 1, event = "modification of both settings")
    assertProjectAware(projectAware2, refresh = 2, event = "modification of both settings")
    assertNotificationAware(projectId1, projectId2, notified = true, event = "modification of both settings")

    refreshProject()
    assertProjectAware(projectAware1, refresh = 2, event = "project refresh")
    assertProjectAware(projectAware2, refresh = 3, event = "project refresh")
    assertNotificationAware(notified = false, event = "project refresh")
  }

  @Test
  fun `test project link-unlink`() {
    simpleTest("settings.groovy") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      settingsFile.appendString("println 'hello'")
      assertState(refresh = 1, notified = true, event = "modification of registered settings")

      removeProjectAware()
      assertState(refresh = 1, subscribe = 1, unsubscribe = 1, notified = false, event = "remove project")

      registerProjectAware()
      assertState(refresh = 2, subscribe = 2, unsubscribe = 1, notified = false, event = "register project without cache")
    }
  }

  @Test
  fun `test external modification tracking`() {
    simpleTest("settings.groovy") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      settingsFile.appendString("println 'settings'")
      assertState(refresh = 1, notified = true, event = "modification of registered settings")

      modification {
        assertState(refresh = 1, notified = false, event = "start external modification")
        settingsFile.replaceString("settings", "modified settings")
        assertState(refresh = 1, notified = false, event = "external modification of registered settings")
      }
      assertState(refresh = 2, notified = false, event = "complete external modification")

      modification {
        assertState(refresh = 2, notified = false, event = "start external modification")
        settingsFile.replaceString("modified settings", "simple settings")
        assertState(refresh = 2, notified = false, event = "external modification of registered settings")
        settingsFile.replaceString("simple settings", "modified settings")
        assertState(refresh = 2, notified = false, event = "external modification of registered settings")
      }
      assertState(refresh = 2, notified = false, event = "complete external modification")

      modification {
        assertState(refresh = 2, notified = false, event = "start external modification")
        settingsFile.delete()
        assertState(refresh = 2, notified = false, event = "external deletion of registered settings")
      }
      assertState(refresh = 3, notified = false, event = "complete external modification")

      modification {
        assertState(refresh = 3, notified = false, event = "start external modification")
        findOrCreateFile("settings.groovy", "println 'settings'")
        assertState(refresh = 3, notified = false, event = "external deletion of registered settings")
      }
      assertState(refresh = 4, notified = false, event = "complete external modification")
    }
  }

  @Test
  fun `test complex external modification tracking`() {
    simpleTest("settings.groovy") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      modification {
        assertState(refresh = 1, notified = false, event = "start first external modification")
        settingsFile.appendString("println 'hello'")
        assertState(refresh = 1, notified = false, event = "first external modification of registered settings")
        modification {
          assertState(refresh = 1, notified = false, event = "start second external modification")
          settingsFile.replaceString("hello", "hi")
          assertState(refresh = 1, notified = false, event = "second external modification of registered settings")
        }
        assertState(refresh = 1, notified = false, event = "complete second external modification")
      }
      assertState(refresh = 2, notified = false, event = "complete first external modification")
    }
  }

  @Test
  fun `test tracker store and restore`() {
    var state = simpleTest("settings.groovy") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      settingsFile.appendString("println 'hello'")
      assertState(refresh = 1, notified = true, event = "modification of registered settings")
      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")
    }

    state = simpleTest("settings.groovy", state = state) { settingsFile ->
      assertState(refresh = 0, notified = false, event = "register project with correct cache")

      settingsFile.replaceString("hello", "hi")
      assertState(refresh = 0, notified = true, event = "modification of registered settings")
      refreshProject()
      assertState(refresh = 1, notified = false, event = "project refresh")
    }

    with(File(projectPath, "settings.groovy")) {
      writeText(readText().replace("hi", "hello"))
    }

    state = simpleTest("settings.groovy", state = state) { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project with external modifications")

      settingsFile.replaceString("hello", "hi")
      assertState(refresh = 1, notified = true, event = "modification of registered settings")
      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")
    }

    state = simpleTest("settings.groovy", state = state) { settingsFile ->
      assertState(refresh = 0, notified = false, event = "register project with correct cache")

      settingsFile.replaceString("hi", "hello")
      assertState(refresh = 0, notified = true, event = "modification of registered settings")
    }

    simpleTest("settings.groovy", state = state) {
      assertState(refresh = 1, notified = false, event = "register project with previous modifications")
    }
  }

  fun `test move and rename settings files`() {
    simpleTest("settings.groovy") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      registerSettingsFile("script.groovy")
      registerSettingsFile("dir/script.groovy")

      var scriptFile = settingsFile.copy("script.groovy")
      assertState(refresh = 1, notified = true, event = "copy to registered settings")

      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")

      scriptFile.delete()
      assertState(refresh = 2, notified = true, event = "delete file")
      scriptFile = settingsFile.copy("script.groovy")
      assertState(refresh = 2, notified = false, event = "revert delete by copy of registered settings")
      val configurationFile = settingsFile.copy("configuration.groovy")
      assertState(refresh = 2, notified = false, event = "copy to registered settings")
      configurationFile.delete()
      assertState(refresh = 2, notified = false, event = "delete file")

      val dir = findOrCreateDirectory("dir")
      assertState(refresh = 2, notified = false, event = "create directory")
      scriptFile.move(dir)
      assertState(refresh = 2, notified = true, event = "move from registered to registered settings")
      scriptFile.move(myProjectRoot)
      assertState(refresh = 2, notified = false, event = "revert move from registered to registered settings")

      settingsFile.rename("configuration.groovy")
      assertState(refresh = 2, notified = true, event = "rename of registered settings")
      settingsFile.rename("settings.groovy")
      assertState(refresh = 2, notified = false, event = "revert rename of registered settings")
    }
  }

  fun `test document changes between save`() {
    simpleTest("settings.groovy") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      val fileDocumentManager = FileDocumentManager.getInstance()
      val settingsDocument = fileDocumentManager.getDocument(settingsFile)!!

      settingsDocument.replaceContent("println 'hello'")
      assertState(refresh = 1, notified = false, event = "document change of registered settings")
      settingsDocument.replaceString("hello", "hi")
      assertState(refresh = 1, notified = false, event = "document change of registered settings")
      settingsDocument.replaceString("hi", "hello")
      assertState(refresh = 1, notified = false, event = "document change of registered settings")
      settingsDocument.save()
      assertState(refresh = 1, notified = true, event = "document save of registered settings")
      settingsDocument.replaceString("hello", "hi")
      assertState(refresh = 1, notified = true, event = "document change after save of registered settings")
      settingsDocument.replaceContent("")
      assertState(refresh = 1, notified = true, event = "document change after save of registered settings")
      settingsDocument.save()
      assertState(refresh = 1, notified = false, event = "document save reverted changes of registered settings")
    }
  }
}
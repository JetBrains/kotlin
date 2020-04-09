// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.Parallel.Companion.parallel
import com.sun.media.jfxmedia.logging.Logger.setLevel
import org.apache.log4j.Level
import org.junit.Test
import java.io.File

class AutoImportTest : AutoImportTestCase() {
  @Test
  fun `test simple modification tracking`() {
    simpleTest("settings.groovy") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      settingsFile.appendString("println 'hello'")
      assertState(refresh = 1, notified = true, event = "modification")
      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")

      settingsFile.replaceString("hello", "hi")
      assertState(refresh = 2, notified = true, event = "modification")
      settingsFile.replaceString("hi", "hello")
      assertState(refresh = 2, notified = false, event = "revert changes")

      settingsFile.appendString("\n ")
      assertState(refresh = 2, notified = false, event = "empty modification")
      settingsFile.replaceString("println", "print ln")
      assertState(refresh = 2, notified = true, event = "split token by space")
      settingsFile.replaceString("print ln", "println")
      assertState(refresh = 2, notified = false, event = "revert modification")

      settingsFile.appendString(" ")
      assertState(refresh = 2, notified = false, event = "empty modification")
      settingsFile.appendString("//It is comment")
      assertState(refresh = 2, notified = false, event = "append comment")
      settingsFile.insertStringAfter("println", "/*It is comment*/")
      assertState(refresh = 2, notified = false, event = "append comment")
      settingsFile.insertString(0, "//")
      assertState(refresh = 2, notified = true, event = "comment code")
      refreshProject()
      assertState(refresh = 3, notified = false, event = "project refresh")

      refreshProject()
      assertState(refresh = 3, notified = false, event = "empty project refresh")
    }
  }

  @Test
  fun `test simple modification tracking in xml`() {
    simpleTest("settings.xml") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      settingsFile.replaceContent("""
        <element>
          <name description="This is a my super name">my-name</name>
        </element>
      """.trimIndent())
      assertState(refresh = 1, notified = true, event = "modification")
      refreshProject()
      assertState(refresh = 2, notified = false, event = "refresh project")

      settingsFile.replaceString("my-name", "my name")
      assertState(refresh = 2, notified = true, event = "replace by space")
      settingsFile.replaceString("my name", "my-name")
      assertState(refresh = 2, notified = false, event = "revert modification")
      settingsFile.replaceString("my-name", "my - name")
      assertState(refresh = 2, notified = true, event = "split token by spaces")
      settingsFile.replaceString("my - name", "my-name")
      assertState(refresh = 2, notified = false, event = "revert modification")
      settingsFile.replaceString("my-name", " my-name ")
      assertState(refresh = 2, notified = false, event = "expand text token by spaces")
      settingsFile.insertStringAfter("</name>", " ")
      assertState(refresh = 2, notified = false, event = "append space after tag")
      settingsFile.insertStringAfter("</name>", "\n  ")
      assertState(refresh = 2, notified = false, event = "append empty line in file")
      settingsFile.replaceString("</name>", "</n am e>")
      assertState(refresh = 2, notified = true, event = "split tag by spaces")
      settingsFile.replaceString("</n am e>", "</name>")
      assertState(refresh = 2, notified = false, event = "revert modification")
      settingsFile.replaceString("</name>", "</ name >")
      assertState(refresh = 2, notified = false, event = "expand tag brackets by spaces")
      settingsFile.replaceString("=", " = ")
      assertState(refresh = 2, notified = false, event = "expand attribute definition")
      settingsFile.replaceString("my super name", "my  super  name")
      assertState(refresh = 2, notified = true, event = "expand space inside attribute value")
      settingsFile.replaceString("my  super  name", "my super name")
      assertState(refresh = 2, notified = false, event = "revert modification")
      settingsFile.insertStringAfter("my super name", " ")
      assertState(refresh = 2, notified = true, event = "insert space in end of attribute")
      settingsFile.replaceString("my super name \"", "my super name\"")
      assertState(refresh = 2, notified = false, event = "revert modification")
    }
  }

  @Test
  fun `test unrecognized settings file`() {
    simpleTest("settings.elvish") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      settingsFile.appendString("q71Gpj5 .9jR°`N.")
      assertState(refresh = 1, notified = true, event = "modification")
      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")

      settingsFile.replaceString("9jR°`N", "9`B")
      assertState(refresh = 2, notified = true, event = "modification")
      settingsFile.replaceString("9`B", "9jR°`N")
      assertState(refresh = 2, notified = false, event = "revert changes")

      settingsFile.appendString(" ")
      assertState(refresh = 2, notified = true, event = "unrecognized empty modification")
      refreshProject()
      assertState(refresh = 3, notified = false, event = "project refresh")
      settingsFile.appendString("//1G iT zt^P1Fp")
      assertState(refresh = 3, notified = true, event = "unrecognized comment modification")
      refreshProject()
      assertState(refresh = 4, notified = false, event = "project refresh")
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

      var newSettingsFile = createVirtualFile("settings.groovy")
      assertState(refresh = 2, notified = true, event = "create registered settings")
      newSettingsFile.replaceContent("println 'hello'")
      assertState(refresh = 2, notified = true, event = "modify registered settings")
      refreshProject()
      assertState(refresh = 3, notified = false, event = "project refresh")

      newSettingsFile.delete()
      assertState(refresh = 3, notified = true, event = "delete registered settings")
      newSettingsFile = createVirtualFile("settings.groovy")
      assertState(refresh = 3, notified = true, event = "create registered settings immediately after deleting")
      newSettingsFile.replaceContent("println 'hello'")
      assertState(refresh = 3, notified = false, event = "modify registered settings immediately after deleting")
    }
  }

  @Test
  fun `test modification tracking with several settings files`() {
    simpleTest("settings.groovy", "println 'hello'") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      val scriptFile = createVirtualFile("script.groovy")
      assertState(refresh = 1, notified = false, event = "create unregistered settings")
      scriptFile.replaceContent("println('hello')")
      assertState(refresh = 1, notified = false, event = "modify unregistered settings")
      registerSettingsFile(scriptFile)
      settingsFile.replaceString("hello", "hi")
      assertState(refresh = 1, notified = true, event = "modification")
      settingsFile.replaceString("hi", "hello")
      assertState(refresh = 1, notified = true, event = "try to revert changes if has other modification")
      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")

      settingsFile.replaceString("hello", "hi")
      assertState(refresh = 2, notified = true, event = "modification")
      scriptFile.replaceString("hello", "hi")
      assertState(refresh = 2, notified = true, event = "modification")
      settingsFile.replaceString("hi", "hello")
      assertState(refresh = 2, notified = true, event = "try to revert changes if has other modification")
      scriptFile.replaceString("hi", "hello")
      assertState(refresh = 2, notified = false, event = "revert changes")
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

    initialize()

    val scriptFile1 = createVirtualFile("script1.groovy")
    val scriptFile2 = createVirtualFile("script2.groovy")

    projectAware1.settingsFiles.add(scriptFile1.path)
    projectAware2.settingsFiles.add(scriptFile2.path)

    register(projectAware1)
    register(projectAware2)

    assertProjectAware(projectAware1, refresh = 1, event = "register project without cache")
    assertProjectAware(projectAware2, refresh = 1, event = "register project without cache")
    assertNotificationAware(event = "register project without cache")

    scriptFile1.appendString("println 1")
    assertProjectAware(projectAware1, refresh = 1, event = "modification of first settings")
    assertProjectAware(projectAware2, refresh = 1, event = "modification of first settings")
    assertNotificationAware(projectId1, event = "modification of first settings")

    scriptFile2.appendString("println 2")
    assertProjectAware(projectAware1, refresh = 1, event = "modification of second settings")
    assertProjectAware(projectAware2, refresh = 1, event = "modification of second settings")
    assertNotificationAware(projectId1, projectId2, event = "modification of second settings")

    scriptFile1.removeContent()
    assertProjectAware(projectAware1, refresh = 1, event = "revert changes at second settings")
    assertProjectAware(projectAware2, refresh = 1, event = "revert changes at second settings")
    assertNotificationAware(projectId2, event = "revert changes at second settings")

    refreshProject()
    assertProjectAware(projectAware1, refresh = 1, event = "project refresh")
    assertProjectAware(projectAware2, refresh = 2, event = "project refresh")
    assertNotificationAware(event = "project refresh")

    scriptFile1.replaceContent("println 'script 1'")
    scriptFile2.replaceContent("println 'script 2'")
    assertProjectAware(projectAware1, refresh = 1, event = "modification of both settings")
    assertProjectAware(projectAware2, refresh = 2, event = "modification of both settings")
    assertNotificationAware(projectId1, projectId2, event = "modification of both settings")

    refreshProject()
    assertProjectAware(projectAware1, refresh = 2, event = "project refresh")
    assertProjectAware(projectAware2, refresh = 3, event = "project refresh")
    assertNotificationAware(event = "project refresh")
  }

  @Test
  fun `test project link-unlink`() {
    simpleTest("settings.groovy") { settingsFile ->
      assertState(refresh = 1, subscribe = 2, unsubscribe = 0, notified = false, event = "register project without cache")

      settingsFile.appendString("println 'hello'")
      assertState(refresh = 1, subscribe = 2, unsubscribe = 0, notified = true, event = "modification")

      removeProjectAware()
      assertState(refresh = 1, subscribe = 2, unsubscribe = 2, notified = false, event = "remove project")

      registerProjectAware()
      assertState(refresh = 2, subscribe = 4, unsubscribe = 2, notified = false, event = "register project without cache")
    }
  }

  @Test
  fun `test external modification tracking`() {
    simpleTest("settings.groovy") {
      var settingsFile = it
      assertState(refresh = 1, notified = false, event = "register project without cache")

      settingsFile.replaceContentInIoFile("println 'hello'")
      assertState(refresh = 2, notified = false, event = "untracked external modification")

      settingsFile.replaceString("hello", "hi")
      assertState(refresh = 2, notified = true, event = "internal modification")

      settingsFile.replaceStringInIoFile("hi", "settings")
      assertState(refresh = 2, notified = true, event = "untracked external modification during internal modification")

      refreshProject()
      assertState(refresh = 3, notified = false, event = "refresh project")

      modification {
        assertState(refresh = 3, notified = false, event = "start external modification")
        settingsFile.replaceStringInIoFile("settings", "modified settings")
        assertState(refresh = 3, notified = false, event = "external modification")
      }
      assertState(refresh = 4, notified = false, event = "complete external modification")

      modification {
        assertState(refresh = 4, notified = false, event = "start external modification")
        settingsFile.replaceStringInIoFile("modified settings", "simple settings")
        assertState(refresh = 4, notified = false, event = "external modification")
        settingsFile.replaceStringInIoFile("simple settings", "modified settings")
        assertState(refresh = 4, notified = false, event = "revert external modification")
      }
      assertState(refresh = 4, notified = false, event = "complete external modification")

      modification {
        assertState(refresh = 4, notified = false, event = "start external modification")
        settingsFile.deleteIoFile()
        assertState(refresh = 4, notified = false, event = "external deletion")
      }
      assertState(refresh = 5, notified = false, event = "complete external modification")

      modification {
        assertState(refresh = 5, notified = false, event = "start external modification")
        settingsFile = createIoFile("settings.groovy")
        assertState(refresh = 5, notified = false, event = "external creation")
        settingsFile.replaceContentInIoFile("println 'settings'")
        assertState(refresh = 5, notified = false, event = "external modification")
      }
      assertState(refresh = 6, notified = false, event = "complete external modification")

      modification {
        assertState(refresh = 6, notified = false, event = "start first external modification")
        settingsFile.replaceStringInIoFile("settings", "hello")
        assertState(refresh = 6, notified = false, event = "first external modification")
        modification {
          assertState(refresh = 6, notified = false, event = "start second external modification")
          settingsFile.replaceStringInIoFile("hello", "hi")
          assertState(refresh = 6, notified = false, event = "second external modification")
        }
        assertState(refresh = 6, notified = false, event = "complete second external modification")
      }
      assertState(refresh = 7, notified = false, event = "complete first external modification")

      modification {
        assertState(refresh = 7, notified = false, event = "start external modification")
        settingsFile.replaceStringInIoFile("println", "print")
        assertState(refresh = 7, notified = false, event = "external modification")
        settingsFile.replaceString("hi", "hello")
        assertState(refresh = 7, notified = true, event = "internal modification during external modification")
        settingsFile.replaceStringInIoFile("hello", "settings")
        assertState(refresh = 7, notified = true, event = "external modification")
      }
      assertState(refresh = 7, notified = true, event = "complete external modification")
      refreshProject()
      assertState(refresh = 8, notified = false, event = "refresh project")
    }
  }

  @Test
  fun `test tracker store and restore`() {
    var state = simpleTest("settings.groovy") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      settingsFile.replaceContent("println 'hello'")
      assertState(refresh = 1, notified = true, event = "modification")
      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")
    }

    state = simpleTest("settings.groovy", state = state) { settingsFile ->
      assertState(refresh = 0, notified = false, event = "register project with correct cache")

      settingsFile.replaceString("hello", "hi")
      assertState(refresh = 0, notified = true, event = "modification")
      refreshProject()
      assertState(refresh = 1, notified = false, event = "project refresh")
    }

    with(File(projectPath, "settings.groovy")) {
      writeText(readText().replace("hi", "hello"))
    }

    state = simpleTest("settings.groovy", state = state) { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project with external modifications")

      settingsFile.replaceString("hello", "hi")
      assertState(refresh = 1, notified = true, event = "modification")
      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")
    }

    state = simpleTest("settings.groovy", state = state) { settingsFile ->
      assertState(refresh = 0, notified = false, event = "register project with correct cache")

      settingsFile.replaceString("hi", "hello")
      assertState(refresh = 0, notified = true, event = "modification")
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
      registerSettingsFile("dir1/script.groovy")
      registerSettingsFile("dir/dir1/script.groovy")

      var scriptFile = settingsFile.copy("script.groovy")
      assertState(refresh = 1, notified = true, event = "copy to registered settings")

      refreshProject()
      assertState(refresh = 2, notified = false, event = "project refresh")

      scriptFile.delete()
      assertState(refresh = 2, notified = true, event = "delete file")
      scriptFile = settingsFile.copy("script.groovy")
      assertState(refresh = 2, notified = false, event = "revert delete by copy")
      val configurationFile = settingsFile.copy("configuration.groovy")
      assertState(refresh = 2, notified = false, event = "copy to registered settings")
      configurationFile.delete()
      assertState(refresh = 2, notified = false, event = "delete file")

      val dir = findOrCreateDirectory("dir")
      val dir1 = findOrCreateDirectory("dir1")
      assertState(refresh = 2, notified = false, event = "create directory")
      scriptFile.move(dir)
      assertState(refresh = 2, notified = true, event = "move settings to directory")
      scriptFile.move(myProjectRoot)
      assertState(refresh = 2, notified = false, event = "revert move settings")
      scriptFile.move(dir1)
      assertState(refresh = 2, notified = true, event = "move settings to directory")
      dir1.move(dir)
      assertState(refresh = 2, notified = true, event = "move directory with settings to other directory")
      scriptFile.move(myProjectRoot)
      assertState(refresh = 2, notified = false, event = "revert move settings")
      scriptFile.move(dir)
      assertState(refresh = 2, notified = true, event = "move settings to directory")
      dir.rename("dir1")
      assertState(refresh = 2, notified = true, event = "rename directory with settings")
      scriptFile.move(myProjectRoot)
      assertState(refresh = 2, notified = false, event = "revert move settings")

      settingsFile.rename("configuration.groovy")
      assertState(refresh = 2, notified = true, event = "rename")
      settingsFile.rename("settings.groovy")
      assertState(refresh = 2, notified = false, event = "revert rename")
    }
  }

  fun `test document changes between save`() {
    simpleTest("settings.groovy") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      val settingsDocument = settingsFile.asDocument()

      settingsDocument.replaceContent("println 'hello'")
      assertState(refresh = 1, notified = true, event = "change")
      refreshProject()
      assertState(refresh = 2, notified = false, event = "refresh project")

      settingsDocument.replaceString("hello", "hi")
      assertState(refresh = 2, notified = true, event = "change")
      settingsDocument.replaceString("hi", "hello")
      assertState(refresh = 2, notified = false, event = "revert change")
      settingsDocument.replaceString("hello", "hi")
      assertState(refresh = 2, notified = true, event = "change")
      settingsDocument.save()
      assertState(refresh = 2, notified = true, event = "save")
      settingsDocument.replaceString("hi", "hello")
      assertState(refresh = 2, notified = false, event = "revert change after save")
      settingsDocument.save()
      assertState(refresh = 2, notified = false, event = "save reverted changes")
    }
  }

  fun `test processing of failure refresh`() {
    simpleTest("settings.groovy") { settingsFile ->
      assertState(refresh = 1, notified = false, event = "register project without cache")

      settingsFile.replaceContentInIoFile("println 'hello'")
      assertState(refresh = 2, notified = false, event = "external change")
      setRefreshStatus(ExternalSystemRefreshStatus.FAILURE)
      settingsFile.replaceStringInIoFile("hello", "hi")
      assertState(refresh = 3, notified = true, event = "external change with failure refresh")
      refreshProject()
      assertState(refresh = 4, notified = true, event = "failure project refresh")
      setRefreshStatus(ExternalSystemRefreshStatus.SUCCESS)
      refreshProject()
      assertState(refresh = 5, notified = false, event = "project refresh")

      settingsFile.replaceString("hi", "hello")
      assertState(refresh = 5, notified = true, event = "modify")
      setRefreshStatus(ExternalSystemRefreshStatus.FAILURE)
      refreshProject()
      assertState(refresh = 6, notified = true, event = "failure project refresh")
      settingsFile.replaceString("hello", "hi")
      assertState(refresh = 6, notified = true, event = "try to revert changes after failure refresh")
      setRefreshStatus(ExternalSystemRefreshStatus.SUCCESS)
      refreshProject()
      assertState(refresh = 7, notified = false, event = "project refresh")
    }
  }

  fun `test files generation during refresh`() {
    val systemId = ProjectSystemId("External System")
    val projectId = ExternalSystemProjectId(systemId, projectPath)
    val projectAware = MockProjectAware(projectId)

    initialize()
    register(projectAware)
    assertProjectAware(projectAware, refresh = 1, event = "register project")
    assertNotificationAware(event = "register project")

    val settingsFile = createIoFile("project.groovy")
    projectAware.onceDuringRefresh {
      projectAware.settingsFiles.add(settingsFile.path)
      settingsFile.replaceContentInIoFile("println 'generated project'")
    }
    forceRefreshProject(projectId)
    assertProjectAware(projectAware, refresh = 2, event = "registration of settings file during project refresh")
    assertNotificationAware(event = "registration of settings file during project refresh")

    // modification during refresh
    projectAware.onceDuringRefresh {
      settingsFile.appendString("println 'hello'")
    }
    forceRefreshProject(projectId)
    assertProjectAware(projectAware, refresh = 3, event = "modification during project refresh")
    assertNotificationAware(projectId, event = "modification during project refresh")
  }

  fun `test disabling of auto-import`() {
    var state = simpleTest("settings.groovy") { settingsFile ->
      assertState(refresh = 1, enabled = true, notified = false, event = "register project without cache")
      disableAutoReloadExternalChanges()
      assertState(refresh = 1, enabled = false, notified = false, event = "disable project auto-import")
      settingsFile.replaceContentInIoFile("println 'hello'")
      assertState(refresh = 1, enabled = false, notified = true, event = "modification with disabled auto-import")
    }
    state = simpleTest("settings.groovy", state = state) { settingsFile ->
      // Open modified project with disabled auto-import for external changes
      assertState(refresh = 0, enabled = false, notified = true, event = "register modified project")
      refreshProject()
      assertState(refresh = 1, enabled = false, notified = false, event = "refresh project")

      // Checkout git branch, that has additional linked project
      withLinkedProject("module/settings.groovy") { moduleSettingsFile ->
        assertState(refresh = 0, enabled = false, notified = true, event = "register project without cache with disabled auto-import")
        moduleSettingsFile.replaceContentInIoFile("println 'hello'")
        assertState(refresh = 0, enabled = false, notified = true, event = "modification with disabled auto-import")
      }
      assertState(refresh = 1, enabled = false, notified = false, event = "remove modified linked project")

      enableAutoReloadExternalChanges()
      assertState(refresh = 1, enabled = true, notified = false, event = "enable auto-import for project without modifications")
      disableAutoReloadExternalChanges()
      assertState(refresh = 1, enabled = false, notified = false, event = "disable project auto-import")

      settingsFile.replaceStringInIoFile("hello", "hi")
      assertState(refresh = 1, enabled = false, notified = true, event = "modification with disabled auto-import")
      enableAutoReloadExternalChanges()
      assertState(refresh = 2, enabled = true, notified = false, event = "enable auto-import for modified project")
    }
    simpleTest("settings.groovy", state = state) {
      assertState(refresh = 0, enabled = true, notified = false, event = "register project with correct cache")
    }
  }

  @Test
  fun `test activation of auto-import`() {
    val systemId = ProjectSystemId("External System")
    val projectId1 = ExternalSystemProjectId(systemId, projectPath)
    val projectId2 = ExternalSystemProjectId(systemId, "$projectPath/sub-project")
    val projectAware1 = MockProjectAware(projectId1)
    val projectAware2 = MockProjectAware(projectId2)

    initialize()

    register(projectAware1, activate = false)
    assertProjectAware(projectAware1, refresh = 0, event = "register project")
    assertNotificationAware(projectId1, event = "register project")
    assertActivationStatus(event = "register project")

    activate(projectId1)
    assertProjectAware(projectAware1, refresh = 1, event = "activate project")
    assertNotificationAware(event = "activate project")
    assertActivationStatus(projectId1, event = "activate project")

    register(projectAware2, activate = false)
    assertProjectAware(projectAware1, refresh = 1, event = "register project 2")
    assertProjectAware(projectAware2, refresh = 0, event = "register project 2")
    assertNotificationAware(projectId2, event = "register project 2")
    assertActivationStatus(projectId1, event = "register project 2")

    val settingsFile1 = createIoFile("settings.groovy")
    val settingsFile2 = createIoFile("sub-project/settings.groovy")
    projectAware1.settingsFiles.add(settingsFile1.path)
    projectAware2.settingsFiles.add(settingsFile2.path)

    settingsFile1.replaceContentInIoFile("println 'hello'")
    settingsFile2.replaceContentInIoFile("println 'hello'")
    assertProjectAware(projectAware1, refresh = 2, event = "externally modified both settings files, but project 2 is inactive")
    assertProjectAware(projectAware2, refresh = 0, event = "externally modified both settings files, but project 2 is inactive")
    assertNotificationAware(projectId2, event = "externally modified both settings files, but project 2 is inactive")
    assertActivationStatus(projectId1, event = "externally modified both settings files, but project 2 is inactive")

    settingsFile1.replaceString("hello", "Hello world!")
    settingsFile2.replaceString("hello", "Hello world!")
    assertProjectAware(projectAware1, refresh = 2, event = "internally modify settings")
    assertProjectAware(projectAware2, refresh = 0, event = "internally modify settings")
    assertNotificationAware(projectId1, projectId2, event = "internally modify settings")
    assertActivationStatus(projectId1, event = "internally modify settings")

    refreshProject()
    assertProjectAware(projectAware1, refresh = 3, event = "refresh project")
    assertProjectAware(projectAware2, refresh = 1, event = "refresh project")
    assertNotificationAware(event = "refresh project")
    assertActivationStatus(projectId1, projectId2, event = "refresh project")
  }

  @Test
  fun `test merging of refreshes with different nature`() {
    Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport").setLevel(Level.DEBUG)
    simpleTest("settings.groovy") { settingsFile ->
      assertState(1, notified = false, event = "register project without cache")

      enableAsyncExecution()

      waitForProjectRefresh {
        parallel {
          thread {
            settingsFile.replaceContentInIoFile("println 'hello'")
          }
          thread {
            forceRefreshProject()
          }
        }
      }

      assertState(refresh = 2, notified = false, event = "modification")
    }
  }
}
// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import org.jetbrains.plugins.gradle.action.ImportProjectFromScriptAction
import org.junit.Test

class GradleImportActionTest: GradleImportingTestCase() {

  @Test fun `check action not visible if not build script`() {
    val virtualFile = createProjectSubFile("some.txt")

    val action = ImportProjectFromScriptAction()
    val defaultContext = DataManager.getInstance().dataContext
    val actionEvent = AnActionEvent.createFromDataContext("ProjectViewPopup", null) { dataId ->
      when {
        CommonDataKeys.PROJECT.`is`(dataId) -> myProject
        CommonDataKeys.VIRTUAL_FILE.`is`(dataId) -> virtualFile
        else -> defaultContext.getData(dataId)
      }
    }

    action.update(actionEvent)
    assertFalse(actionEvent.presentation.isVisible)
  }


  @Test
  fun `check action visibility for imported project`() {

    val config = createProjectConfig(injectRepo("apply plugin: 'java'"))

    val action = ImportProjectFromScriptAction()
    val defaultContext = DataManager.getInstance().dataContext
    val actionEvent = AnActionEvent.createFromDataContext("ProjectViewPopup", null) { dataId ->
      when {
        CommonDataKeys.PROJECT.`is`(dataId) -> myProject
        CommonDataKeys.VIRTUAL_FILE.`is`(dataId) -> config
        else -> defaultContext.getData(dataId)
      }
    }

    action.update(actionEvent)
    assertTrue("Action should be visible if project is not imported", actionEvent.presentation.isVisible)

    importProject()

    action.update(actionEvent)
    assertFalse("Action should not be visible for imported project", actionEvent.presentation.isVisible)
  }
}
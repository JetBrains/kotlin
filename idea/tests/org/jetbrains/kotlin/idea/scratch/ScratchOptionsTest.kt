/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import org.jetbrains.kotlin.idea.scratch.ui.ScratchTopPanel
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.Assert
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class ScratchOptionsTest : AbstractScratchRunActionTest() {

    fun testOptionsSaveOnClosingFile() {
        val scratchPanelBeforeClosingFile = configureScratchByText("scratch_1.kts", testScratchText())

        val newIsReplValue = !scratchPanelBeforeClosingFile.scratchFile.options.isRepl
        val newIsMakeBeforeRunValue = !scratchPanelBeforeClosingFile.scratchFile.options.isMakeBeforeRun
        val newIsInteractiveModeValue = !scratchPanelBeforeClosingFile.scratchFile.options.isInteractiveMode

        scratchPanelBeforeClosingFile.setReplMode(newIsReplValue)
        scratchPanelBeforeClosingFile.setMakeBeforeRun(newIsMakeBeforeRunValue)
        scratchPanelBeforeClosingFile.setInteractiveMode(newIsInteractiveModeValue)

        myManager.closeFile(myFixture.file.virtualFile)
        myManager.openFile(myFixture.file.virtualFile, true)

        val scratchPanelAfterClosingFile = getScratchPanelFromEditorSelectedForFile(myManager, myFixture.file.virtualFile) ?: error("Couldn't find scratch panel")

        Assert.assertEquals("Wrong value for isRepl checkbox", newIsReplValue, scratchPanelAfterClosingFile.scratchFile.options.isRepl)
        Assert.assertEquals(
            "Wrong value for isMakeBeforeRun checkbox",
            newIsMakeBeforeRunValue,
            scratchPanelAfterClosingFile.scratchFile.options.isMakeBeforeRun
        )
        Assert.assertEquals(
            "Wrong value for isInteractiveMode checkbox",
            newIsInteractiveModeValue,
            scratchPanelAfterClosingFile.scratchFile.options.isInteractiveMode
        )
    }

    fun testModuleSelectionPanelIsVisibleForScratchFile() {
        val scratchTopPanel = configureScratchByText("scratch_1.kts", testScratchText())

        Assert.assertTrue("Module selector should be visible for scratches", isModuleSelectorVisible(scratchTopPanel))
    }

    fun testModuleSelectionPanelIsHiddenForWorksheetFile() {
        val scratchTopPanel = configureWorksheetByText("worksheet.ws.kts", testScratchText())

        Assert.assertFalse("Module selector should be hidden for worksheets", isModuleSelectorVisible(scratchTopPanel))
    }

    fun testCurrentModuleIsAutomaticallySelectedForWorksheetFile() {
        val scratchFile = configureWorksheetByText("worksheet.ws.kts", testScratchText()).scratchFile

        Assert.assertEquals(
            "Selected module should be equal to current project module for worksheets",
            myFixture.module,
            scratchFile.module
        )
    }

    private fun isModuleSelectorVisible(scratchTopPanel: ScratchTopPanel): Boolean {
        return getActionVisibility(scratchTopPanel.getModuleSelectorAction())
    }

}
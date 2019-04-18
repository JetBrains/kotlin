/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import org.jetbrains.kotlin.idea.scratch.ui.ScratchTopPanel
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.Assert
import org.junit.runner.RunWith
import javax.swing.JCheckBox
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class ScratchOptionsSaveTest : AbstractScratchRunActionTest() {

    fun testOptionsSaveOnClosingFile() {
        val scratchFile = createScratchFile("scratch_1.kts", "val a = 1")
        val (_, scratchPanelBeforeClosingFile) = getEditorWithScratchPanel(myManager, scratchFile) ?: error("Couldn't find scratch panel")

        Assert.assertEquals(
            "This test checks that checkbox options are restored after file closing. Not all checkboxes are checked in this test",
            3,
            ScratchTopPanel::class.declaredMemberProperties.filter { it.returnType == JCheckBox::class.createType() }.size
        )

        val newIsReplValue = !scratchPanelBeforeClosingFile.scratchFile.options.isRepl
        val newIsMakeBeforeRunValue = !scratchPanelBeforeClosingFile.scratchFile.options.isMakeBeforeRun
        val newIsInteractiveModeValue = !scratchPanelBeforeClosingFile.scratchFile.options.isInteractiveMode

        scratchPanelBeforeClosingFile.setReplMode(newIsReplValue)
        scratchPanelBeforeClosingFile.setMakeBeforeRun(newIsMakeBeforeRunValue)
        scratchPanelBeforeClosingFile.setInteractiveMode(newIsInteractiveModeValue)

        myManager.closeFile(scratchFile)
        myManager.openFile(scratchFile, true)

        val (_, scratchPanelAfterClosingFile) = getEditorWithScratchPanel(myManager, scratchFile) ?: error("Couldn't find scratch panel")

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
}
/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import org.jetbrains.kotlin.idea.scratch.ui.ModulesComboBoxAction
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.Assert
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class ScratchOptionsTest : AbstractScratchRunActionTest() {

    fun testModuleSelectionPanelIsVisibleForScratchFile() {
        val scratchFile = configureScratchByText("scratch_1.kts", testScratchText())

        Assert.assertTrue("Module selector should be visible for scratches", isModuleSelectorVisible(scratchFile))
    }

    fun testModuleSelectionPanelIsHiddenForWorksheetFile() {
        val scratchFile = configureWorksheetByText("worksheet.ws.kts", testScratchText())

        Assert.assertFalse("Module selector should be hidden for worksheets", isModuleSelectorVisible(scratchFile))
    }

    fun testCurrentModuleIsAutomaticallySelectedForWorksheetFile() {
        val scratchFile = configureWorksheetByText("worksheet.ws.kts", testScratchText())

        Assert.assertEquals(
            "Selected module should be equal to current project module for worksheets",
            myFixture.module,
            scratchFile.module
        )
    }

    private fun isModuleSelectorVisible(scratchTopPanel: ScratchFile): Boolean {
        return ModulesComboBoxAction(scratchTopPanel).isModuleSelectorVisible()
    }

}
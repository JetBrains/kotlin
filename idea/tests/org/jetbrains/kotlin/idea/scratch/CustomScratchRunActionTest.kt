/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable
import com.intellij.openapi.roots.libraries.Library
import org.jetbrains.kotlin.idea.run.createLibraryWithLongPaths
import org.jetbrains.kotlin.idea.scratch.output.InlayScratchFileRenderer
import org.jetbrains.kotlin.idea.util.application.runWriteAction

class CustomScratchRunActionTest : AbstractScratchRunActionTest() {

    fun testLongCommandLineWithRepl() {
        assertEquals("RESULT: res0: kotlin.Int = 1", getOutput(true))
    }

    fun testLongCommandLine() {
        assertEquals("RESULT: 1", getOutput(false))
    }

    private fun getOutput(isRepl: Boolean): String {
        val fileText = "1"
        val scratchFile = createScratchFile("scratch_1.kts", fileText)

        val (editor, scratchPanel) = getEditorWithScratchPanel(myManager, scratchFile) ?: error("Couldn't find scratch panel")
        scratchPanel.scratchFile.saveOptions {
            copy(isRepl = isRepl, isInteractiveMode = false)
        }

        scratchPanel.setModule(myFixture.module)

        launchScratch(scratchFile)

        return editor.editor.inlayModel.getInlineElementsInRange(0, fileText.length)
            .map { it.renderer }
            .filterIsInstance<InlayScratchFileRenderer>()
            .joinToString().trim()
    }

    private val library: Library by lazy {
        createLibraryWithLongPaths(project)
    }

    override fun setUp() {
        super.setUp()

        ModuleRootModificationUtil.addDependency(myFixture.module, library)
    }

    override fun tearDown() {
        removeLibraryWithLongPaths()

        super.tearDown()
    }

    private fun removeLibraryWithLongPaths() {
        runWriteAction {
            val modifiableModel = ProjectLibraryTable.getInstance(project).modifiableModel
            try {
                modifiableModel.removeLibrary(library)
            } finally {
                modifiableModel.commit()
            }
        }
    }
}
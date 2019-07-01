/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable
import com.intellij.openapi.roots.libraries.Library
import org.jetbrains.kotlin.idea.run.createLibraryWithLongPaths
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class CustomScratchRunActionTest : AbstractScratchRunActionTest() {

    fun testLongCommandLineWithRepl() {
        assertEquals(
            """|// REPL_MODE: true
               |// INTERACTIVE_MODE: false
               |1    // RESULT: res0: kotlin.Int = 1""".trimMargin(),
            getOutput(true)
        )
    }

    fun testLongCommandLine() {
        assertEquals(
            """|// REPL_MODE: false
               |// INTERACTIVE_MODE: false
               |1    // RESULT: 1""".trimMargin(),
            getOutput(false)
        )
    }

    private fun getOutput(isRepl: Boolean): String {
        val fileText = testScratchText().inlinePropertiesValues(isRepl)
        configureScratchByText("scratch_1.kts", fileText)

        launchScratch()
        waitUntilScratchFinishes()

        return getFileTextWithInlays()
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
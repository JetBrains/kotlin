/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.folding

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.FoldRegion
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.folding.AbstractKotlinFoldingTest.doTestWithSettings
import org.jetbrains.kotlin.idea.imports.KotlinImportOptimizer

class FoldingAfterOptimizeImportsTest : AbstractKotlinFoldingTest() {
    private val fixture: JavaCodeInsightTestFixture
        get() = myFixture!!

    private val fileText: String
        get() = fixture.file!!.text!!

    fun testFoldingAfterOptimizeImports() {
        doTest()
    }

    fun testFoldingAfterOptimizeImportsRemoveFirst() {
        doTest()
    }

    override fun fileName(): String = getTestName(true) + ".kt"

    private fun doTest() {
        fixture.configureByFile(fileName())

        doTestWithSettings(fileText) {
            CodeFoldingManager.getInstance(fixture.project)!!.buildInitialFoldings(editor)
            getFoldingRegion(0).checkRegion(false, findStringWithPrefixes("// REGION BEFORE: "))

            CommandProcessor.getInstance()?.executeCommand(fixture.project,
                                                           KotlinImportOptimizer().processFile(fixture.file),
                                                           "Optimize Imports", null,
                                                           UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
            getFoldingRegion(0).checkRegion(false, findStringWithPrefixes("// REGION AFTER: "))
            null
        }
    }

    private fun getFoldingRegion(number: Int): FoldRegion {
        fixture.doHighlighting()
        val model = editor.foldingModel
        val foldingRegions = model.allFoldRegions
        assert(foldingRegions.size >= number) { "There is no enough folding regions in file: in file - ${foldingRegions.size} , expected = ${number}" }
        return foldingRegions[number]
    }

    override fun getTestDataPath() = File(PluginTestCaseBase.getTestDataPathBase(), "/folding/afterOptimizeImports/").path + File.separator

    private fun findStringWithPrefixes(prefix: String) = InTextDirectivesUtils.findStringWithPrefixes(fileText, prefix)
                                                            ?: throw AssertionError("Couldn't find line with prefix $prefix")

    private fun FoldRegion.getPosition() = "${startOffset}:${endOffset}"

    private fun FoldRegion.checkRegion(isExpanded: Boolean, position: String): Unit {
        assert(isValid) { "Region should be valid: $this" }
        assert(isExpanded == isExpanded()) { "isExpanded should be $isExpanded. Actual = ${isExpanded()}" }
        assert(position == getPosition()) { "Region position is wrong: expected = $position, actual = ${getPosition()}" }
    }

}

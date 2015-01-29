/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.folding

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.FoldRegion
import org.jetbrains.kotlin.idea.PluginTestCaseBase
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
        get() = fixture.getFile()!!.getText()!!

    fun testFoldingAfterOptimizeImports() {
        doTest()
    }

    fun testFoldingAfterOptimizeImportsRemoveFirst() {
        doTest()
    }

    private fun doTest() {
        fixture.configureByFile(getTestName(true) + ".kt")

        doTestWithSettings(fileText) {
            fileText ->
            CodeFoldingManager.getInstance(fixture.getProject())!!.buildInitialFoldings(getEditor())
            getFoldingRegion(0).checkRegion(false, findStringWithPrefixes("// REGION BEFORE: "))

            CommandProcessor.getInstance()?.executeCommand(fixture.getProject(),
                                                           KotlinImportOptimizer().processFile(fixture.getFile()),
                                                           "Optimize Imports", null,
                                                           UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
            getFoldingRegion(0).checkRegion(false, findStringWithPrefixes("// REGION AFTER: "))
            null
        }
    }

    private fun getFoldingRegion(number: Int): FoldRegion {
        fixture.doHighlighting()
        val model = getEditor().getFoldingModel()
        val foldingRegions = model.getAllFoldRegions()
        assert(foldingRegions.size >= number) { "There is no enough folding regions in file: in file - ${foldingRegions.size} , expected = ${number}" }
        return foldingRegions[number]
    }

    override fun getTestDataPath() = File(PluginTestCaseBase.getTestDataPathBase(), "/folding/afterOptimizeImports/").getPath() + File.separator

    private fun findStringWithPrefixes(prefix: String) = InTextDirectivesUtils.findStringWithPrefixes(fileText, prefix)
                                                            ?: throw AssertionError("Couldn't find line with prefix $prefix")

    private fun FoldRegion.getPosition() = "${getStartOffset()}:${getEndOffset()}"

    private fun FoldRegion.checkRegion(isExpanded: Boolean, position: String): Unit {
        assert(isValid()) { "Region should be valid: $this" }
        assert(isExpanded == isExpanded()) { "isExpanded should be $isExpanded. Actual = ${isExpanded()}" }
        assert(position == getPosition()) { "Region position is wrong: expected = $position, actual = ${getPosition()}" }
    }

}

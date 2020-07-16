/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.folding

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.editor.FoldRegion
import org.jetbrains.kotlin.idea.imports.KotlinImportOptimizer
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.internal.runners.JUnit38ClassRunner
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.TestRoot
import org.junit.runner.RunWith

@TestRoot("idea")
@TestMetadata("testData/folding/afterOptimizeImports")
@RunWith(JUnit38ClassRunner::class)
class FoldingAfterOptimizeImportsTest : AbstractKotlinFoldingTest() {
    fun testFoldingAfterOptimizeImports() = doTest()
    fun testFoldingAfterOptimizeImportsRemoveFirst() = doTest()

    private val fileText: String
        get() = myFixture.file.text

    private fun doTest() {
        myFixture.configureByFile(fileName())

        doTestWithSettings(fileText) {
            CodeFoldingManager.getInstance(myFixture.project)!!.buildInitialFoldings(editor)
            getFoldingRegion(0).checkRegion(false, findStringWithPrefixes("// REGION BEFORE: "))

            myFixture.project.executeWriteCommand(
                "Optimize import in tests"
            ) { KotlinImportOptimizer().processFile(myFixture.file).run() }

            getFoldingRegion(0).checkRegion(false, findStringWithPrefixes("// REGION AFTER: "))
        }
    }

    private fun getFoldingRegion(@Suppress("SameParameterValue") number: Int): FoldRegion {
        myFixture.doHighlighting()
        val model = editor.foldingModel
        val foldingRegions = model.allFoldRegions
        assert(foldingRegions.size >= number) {
            "There is no enough folding regions in file: in file - ${foldingRegions.size} , expected = $number"
        }
        return foldingRegions[number]
    }

    private fun findStringWithPrefixes(prefix: String) =
        InTextDirectivesUtils.findStringWithPrefixes(fileText, prefix)
            ?: throw AssertionError("Couldn't find line with prefix $prefix")

    private fun FoldRegion.getPosition() = "$startOffset:$endOffset"

    private fun FoldRegion.checkRegion(isExpanded: Boolean, position: String) {
        assert(isValid) { "Region should be valid: $this" }
        assert(isExpanded == isExpanded()) { "isExpanded should be $isExpanded. Actual = ${isExpanded()}" }
        assert(position == getPosition()) { "Region position is wrong: expected = $position, actual = ${getPosition()}" }
    }

}

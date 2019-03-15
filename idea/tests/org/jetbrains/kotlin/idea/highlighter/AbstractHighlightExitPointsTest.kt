/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.highlighting.HighlightUsagesHandler
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import kotlin.test.assertEquals

abstract class AbstractHighlightExitPointsTest : LightCodeInsightFixtureTestCase() {
    fun doTest(testDataPath: String) {
        myFixture.configureByFile(testDataPath)
        HighlightUsagesHandler.invoke(myFixture.project, myFixture.editor, myFixture.file)

        val text = myFixture.file.text
        val expectedToBeHighlighted = InTextDirectivesUtils.findLinesWithPrefixesRemoved(text, "//HIGHLIGHTED:")
        val searchResultsTextAttributes = EditorColorsManager.getInstance().globalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)
        val highlighters = myFixture.editor.markupModel.allHighlighters
                .filter { it.textAttributes == searchResultsTextAttributes }
        val actual = highlighters.map { text.substring(it.startOffset, it.endOffset) }
        assertEquals(expectedToBeHighlighted, actual)
    }
}
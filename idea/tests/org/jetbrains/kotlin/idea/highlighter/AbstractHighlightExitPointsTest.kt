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
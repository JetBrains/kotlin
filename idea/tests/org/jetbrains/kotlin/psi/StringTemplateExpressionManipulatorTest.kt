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

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetLightProjectDescriptor
import com.intellij.psi.ElementManipulators
import org.junit.Assert.*
import com.intellij.openapi.util.TextRange

public class StringTemplateExpressionManipulatorTest : JetLightCodeInsightFixtureTestCase() {
    public fun testSingleQuoted() {
        doTestContentChange("\"a\"", "b", "\"b\"")
        doTestContentChange("\"\"", "b", "\"b\"")
        doTestContentChange("\"a\"", "\t", "\"\\t\"")
        doTestContentChange("\"a\"", "\n", "\"\\n\"")
        doTestContentChange("\"a\"", "\\t", "\"\\\\t\"")
    }

    public fun testUnclosedQuoted() {
        doTestContentChange("\"a", "b", "\"b")
        doTestContentChange("\"", "b", "\"b")
        doTestContentChange("\"a", "\t", "\"\\t")
        doTestContentChange("\"a", "\n", "\"\\n")
        doTestContentChange("\"a", "\\t", "\"\\\\t")
    }

    public fun testTripleQuoted() {
        doTestContentChange("\"\"\"a\"\"\"", "b", "\"\"\"b\"\"\"")
        doTestContentChange("\"\"\"\"\"\"", "b", "\"\"\"b\"\"\"")
        doTestContentChange("\"\"\"a\"\"\"", "\t", "\"\"\"\t\"\"\"")
        doTestContentChange("\"\"\"a\"\"\"", "\n", "\"\"\"\n\"\"\"")
        doTestContentChange("\"\"\"a\"\"\"", "\\t", "\"\"\"\\t\"\"\"")
    }

    public fun testUnclosedTripleQuoted() {
        doTestContentChange("\"\"\"a", "b", "\"\"\"b")
        doTestContentChange("\"\"\"", "b", "\"\"\"b")
        doTestContentChange("\"\"\"a", "\t", "\"\"\"\t")
        doTestContentChange("\"\"\"a", "\n", "\"\"\"\n")
        doTestContentChange("\"\"\"a", "\\t", "\"\"\"\\t")
    }

    public fun testReplaceRange() {
        doTestContentChange("\"abc\"", "x", range = TextRange(2,3), expected = "\"axc\"")
        doTestContentChange("\"\"\"abc\"\"\"", "x", range = TextRange(4,5), expected = "\"\"\"axc\"\"\"")
    }

    private fun doTestContentChange(original: String, newContent: String, expected: String, range: TextRange? = null) {
        val expression = JetPsiFactory(getProject()).createExpression(original) as JetStringTemplateExpression
        val manipulator = ElementManipulators.getNotNullManipulator(expression)
        val newExpression = if (range == null) manipulator.handleContentChange(expression, newContent) else manipulator.handleContentChange(expression, range, newContent)
        assertEquals(expected, newExpression.getText())
    }

    override fun getProjectDescriptor() = JetLightProjectDescriptor.INSTANCE
}

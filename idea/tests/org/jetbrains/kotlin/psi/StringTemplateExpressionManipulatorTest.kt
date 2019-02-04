/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.testFramework.LoggedErrorProcessor
import org.apache.log4j.Logger
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor

class StringTemplateExpressionManipulatorTest : KotlinLightCodeInsightFixtureTestCase() {
    fun testSingleQuoted() {
        doTestContentChange("\"a\"", "b", "\"b\"")
        doTestContentChange("\"\"", "b", "\"b\"")
        doTestContentChange("\"a\"", "\t", "\"\\t\"")
        doTestContentChange("\"a\"", "\n", "\"\\n\"")
        doTestContentChange("\"a\"", "\\t", "\"\\\\t\"")
    }

    fun testUnclosedQuoted() {
        doTestContentChange("\"a", "b", "\"b")
        doTestContentChange("\"", "b", "\"b")
        doTestContentChange("\"a", "\t", "\"\\t")
        doTestContentChange("\"a", "\n", "\"\\n")
        doTestContentChange("\"a", "\\t", "\"\\\\t")
    }

    fun testTripleQuoted() {
        doTestContentChange("\"\"\"a\"\"\"", "b", "\"\"\"b\"\"\"")
        doTestContentChange("\"\"\"\"\"\"", "b", "\"\"\"b\"\"\"")
        doTestContentChange("\"\"\"a\"\"\"", "\t", "\"\"\"\t\"\"\"")
        doTestContentChange("\"\"\"a\"\"\"", "\n", "\"\"\"\n\"\"\"")
        doTestContentChange("\"\"\"a\"\"\"", "\\t", "\"\"\"\\t\"\"\"")
    }

    fun testUnclosedTripleQuoted() {
        doTestContentChange("\"\"\"a", "b", "\"\"\"b")
        doTestContentChange("\"\"\"", "b", "\"\"\"b")
        doTestContentChange("\"\"\"a", "\t", "\"\"\"\t")
        doTestContentChange("\"\"\"a", "\n", "\"\"\"\n")
        doTestContentChange("\"\"\"a", "\\t", "\"\"\"\\t")
    }

    fun testReplaceRange() {
        doTestContentChange("\"abc\"", "x", range = TextRange(2,3), expected = "\"axc\"")
        doTestContentChange("\"\"\"abc\"\"\"", "x", range = TextRange(4,5), expected = "\"\"\"axc\"\"\"")
        doTestContentChange(
            "\"<div style = \\\"default\\\">\${foo(\"\")}</div>\"",
            "custom", range = TextRange(16, 23),
            expected = "\"<div style = \\\"custom\\\">\${foo(\"\")}</div>\""
        )
    }


    fun testHackyReplaceRange() {
        suppressFallingOnLogError {
            doTestContentChange("\"a\\\"bc\"", "'", range = TextRange(0, 4), expected = "'bc\"")
        }
    }

    fun testTemplateWithInterpolation() {
        doTestContentChange("\"<div>\${foo(\"\")}</div>\"", "<p>\${foo(\"\")}</p>", "\"<p>\${foo(\"\")}</p>\"")
        doTestContentChange(
            "\"<div style = \\\"default\\\">\${foo(\"\")}</div>\"",
            "<p style = \"custom\">\${foo(\"\")}</p>",
            "\"<p style = \\\"custom\\\">\${foo(\"\")}</p>\""
        )
    }

    private fun doTestContentChange(original: String, newContent: String, expected: String, range: TextRange? = null) {
        val expression = KtPsiFactory(project).createExpression(original) as KtStringTemplateExpression
        val manipulator = ElementManipulators.getNotNullManipulator(expression)
        val newExpression = if (range == null) manipulator.handleContentChange(expression, newContent) else manipulator.handleContentChange(expression, range, newContent)
        assertEquals(expected, newExpression?.text)
    }

    override fun getProjectDescriptor() = KotlinLightProjectDescriptor.INSTANCE
}

private fun <T> suppressFallingOnLogError(call: () -> T) {
    val loggedErrorProcessor = LoggedErrorProcessor.getInstance()
    try {
        LoggedErrorProcessor.setNewInstance(object : LoggedErrorProcessor() {
            override fun processError(message: String?, t: Throwable?, details: Array<out String>?, logger: Logger) {}
        })
        call()
    } finally {
        LoggedErrorProcessor.setNewInstance(loggedErrorProcessor)
    }
}
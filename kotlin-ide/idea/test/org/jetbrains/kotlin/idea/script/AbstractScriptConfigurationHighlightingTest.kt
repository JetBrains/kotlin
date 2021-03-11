/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.codeInsight.highlighting.HighlightUsagesHandler
import com.intellij.testFramework.exceptionCases.AbstractExceptionCase
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.ComparisonFailure

abstract class AbstractScriptConfigurationHighlightingTest : AbstractScriptConfigurationTest() {
    fun doTest(unused: String) {
        configureScriptFile(testDataFile())

        // Highlight references at caret
        HighlightUsagesHandler.invoke(project, editor, myFile)

        checkHighlighting(
            editor,
            InTextDirectivesUtils.isDirectiveDefined(file.text, "// CHECK_WARNINGS"),
            InTextDirectivesUtils.isDirectiveDefined(file.text, "// CHECK_INFOS")
        )
    }

    fun doComplexTest(unused: String) {
        configureScriptFile(testDataFile())
        assertException(object : AbstractExceptionCase<ComparisonFailure>() {
            override fun getExpectedExceptionClass(): Class<ComparisonFailure> = ComparisonFailure::class.java

            override fun tryClosure() {
                checkHighlighting(editor, false, false)
            }
        })

        ScriptConfigurationManager.updateScriptDependenciesSynchronously(myFile)
        checkHighlighting(editor, false, false)
    }
}
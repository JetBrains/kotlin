/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.codeInsight.unwrap

import com.intellij.codeInsight.unwrap.Unwrapper
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractUnwrapRemoveTest : KotlinLightCodeInsightFixtureTestCase() {
    fun doTestExpressionRemover(unused: String) = doTest(KotlinUnwrappers.KotlinExpressionRemover::class.java)
    fun doTestThenUnwrapper(unused: String) = doTest(KotlinUnwrappers.KotlinThenUnwrapper::class.java)
    fun doTestElseUnwrapper(unused: String) = doTest(KotlinUnwrappers.KotlinElseUnwrapper::class.java)
    fun doTestElseRemover(unused: String) = doTest(KotlinUnwrappers.KotlinElseRemover::class.java)
    fun doTestLoopUnwrapper(unused: String) = doTest(KotlinUnwrappers.KotlinLoopUnwrapper::class.java)
    fun doTestTryUnwrapper(unused: String) = doTest(KotlinUnwrappers.KotlinTryUnwrapper::class.java)
    fun doTestCatchUnwrapper(unused: String) = doTest(KotlinUnwrappers.KotlinCatchUnwrapper::class.java)
    fun doTestCatchRemover(unused: String) = doTest(KotlinUnwrappers.KotlinCatchRemover::class.java)
    fun doTestFinallyUnwrapper(unused: String) = doTest(KotlinUnwrappers.KotlinFinallyUnwrapper::class.java)
    fun doTestFinallyRemover(unused: String) = doTest(KotlinUnwrappers.KotlinFinallyRemover::class.java)
    fun doTestLambdaUnwrapper(unused: String) = doTest(KotlinLambdaUnwrapper::class.java)
    fun doTestFunctionParameterUnwrapper(unused: String) = doTest(KotlinFunctionParameterUnwrapper::class.java)

    private fun doTest(unwrapperClass: Class<out Unwrapper>) {
        val testFile = testDataFile()

        myFixture.configureByFile(testFile)

        val fileText = file.text
        val isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// IS_APPLICABLE: ")
        val isApplicableExpected = isApplicableString == null || isApplicableString == "true"
        val option = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// OPTION: ")
        val optionIndex = option?.toInt() ?: 0

        val unwrappersWithPsi = KotlinUnwrapDescriptor().collectUnwrappers(project, editor, file)
        if (isApplicableExpected) {
            val selectedUnwrapperWithPsi = unwrappersWithPsi[optionIndex]
            assertEquals(unwrapperClass, selectedUnwrapperWithPsi.second.javaClass)
            val first = selectedUnwrapperWithPsi.first

            executeCommand(project, "Test") {
                runWriteAction {
                    selectedUnwrapperWithPsi.second.unwrap(editor, first)
                }
            }

            val expectedFile = File(testFile.parent, testFile.name + ".after")
            myFixture.checkResultByFile(expectedFile)
        } else {
            assert(unwrappersWithPsi.all { it.second.javaClass != unwrapperClass })
        }
    }
}
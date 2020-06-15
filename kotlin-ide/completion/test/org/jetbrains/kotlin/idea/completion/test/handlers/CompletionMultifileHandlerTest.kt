/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test.handlers

import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.kotlin.idea.completion.test.COMPLETION_TEST_DATA_BASE_PATH
import org.jetbrains.kotlin.idea.completion.test.KotlinCompletionTestCase
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit38ClassRunner::class)
class CompletionMultiFileHandlerTest : KotlinCompletionTestCase() {
    fun testExtensionFunctionImport() {
        doTest()
    }

    fun testExtensionPropertyImport() {
        doTest()
    }

    fun testImportAlreadyImportedObject() {
        doTest()
    }

    fun testJetClassCompletionImport() {
        doTest()
    }

    fun testStaticMethodFromGrandParent() {
        doTest('\n', "StaticMethodFromGrandParent-1.java", "StaticMethodFromGrandParent-2.java")
    }

    fun testTopLevelFunctionImport() {
        doTest()
    }

    fun testTopLevelFunctionInQualifiedExpr() {
        doTest()
    }

    fun testTopLevelPropertyImport() {
        doTest()
    }

    fun testTopLevelValImportInStringTemplate() {
        doTest()
    }

    fun testNoParenthesisInImports() {
        doTest()
    }

    fun testKeywordExtensionFunctionName() {
        doTest()
    }

    fun testInfixExtensionCallImport() {
        doTest()
    }

    fun testClassWithClassObject() {
        doTest()
    }

    fun testGlobalFunctionImportInLambda() {
        doTest()
    }

    fun testObjectInStringTemplate() {
        doTest()
    }

    fun testPropertyFunctionConflict() {
        doTest()
    }

    fun testPropertyFunctionConflict2() {
        doTest(tailText = " { Int, Int -> ... } (i: (Int, Int) -> Unit) (a.b)")
    }

    fun testExclCharInsertImport() {
        doTest('!')
    }

    fun testPropertyKeysWithPrefixEnter() {
        doTest('\n', "TestBundle.properties")
    }

    fun testPropertyKeysWithPrefixTab() {
        doTest('\t', "TestBundle.properties")
    }

    fun testFileRefInStringLiteralEnter() {
        doTest('\n', "foo.txt", "bar.txt")
    }

    fun testFileRefInStringLiteralTab() {
        doTest('\t', "foo.txt", "bar.txt")
    }

    fun testNotImportedExtension() {
        doTest()
    }

    fun testNotImportedTypeAlias() {
        doTest()
    }

    fun testKT12077() {
        doTest()
    }

    fun doTest(completionChar: Char = '\n', vararg extraFileNames: String, tailText: String? = null) {
        val fileName = getTestName(false)

        val defaultFiles = listOf("$fileName-1.kt", "$fileName-2.kt")
        val filteredFiles = defaultFiles.filter { File(testDataPath, it).exists() }

        require(filteredFiles.isNotEmpty()) { "At least one of $defaultFiles should exist!" }

        configureByFiles(null, *extraFileNames)
        configureByFiles(null, *filteredFiles.toTypedArray())
        complete(2)
        if (myItems != null) {
            val item = if (tailText == null)
                myItems.singleOrNull() ?: error("Multiple items in completion")
            else {
                val presentation = LookupElementPresentation()
                myItems.first {
                    it.renderElement(presentation)
                    presentation.tailText == tailText
                } ?: error("Tail text not found")
            }

            selectItem(item, completionChar)
        }
        checkResultByFile("$fileName.kt.after")
    }

    override fun getTestDataPath() = File(COMPLETION_TEST_DATA_BASE_PATH, "/handlers/multifile/").path + File.separator
}

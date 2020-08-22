/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test.handlers

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.completion.test.COMPLETION_TEST_DATA_BASE
import org.jetbrains.kotlin.idea.completion.test.KotlinFixtureCompletionBaseTestCase
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit38ClassRunner::class)
class SmartCompletionMultifileHandlerTest : KotlinFixtureCompletionBaseTestCase() {
    fun testImportExtensionFunction() {
        doTest()
    }

    fun testImportExtensionProperty() {
        doTest()
    }

    fun testAnonymousObjectGenericJava() {
        doTest()
    }

    fun testImportAnonymousObject() {
        doTest()
    }

    fun testNestedSamAdapter() {
        doTest(lookupString = "Nested")
    }

    private fun doTest(lookupString: String? = null, itemText: String? = null) {
        val fileName = getTestName(false)

        val fileNames = listOf("$fileName-1.kt", "$fileName-2.kt", "$fileName.java")

        myFixture.configureByFiles(*fileNames.filter { File(testDataPath + it).exists() }.toTypedArray())

        val items = complete(CompletionType.SMART, 1)
        if (items != null) {
            fun isMatching(lookupElement: LookupElement): Boolean {
                if (lookupString != null && lookupElement.lookupString != lookupString) return false

                val presentation = LookupElementPresentation()
                lookupElement.renderElement(presentation)
                if (itemText != null && presentation.itemText != itemText) return false

                return true
            }

            val matchedItems = items.filter(::isMatching)
            when (matchedItems.size) {
                0 -> fail("No matching items found")
                1 -> CompletionHandlerTestBase.selectItem(myFixture, items[0], Lookup.NORMAL_SELECT_CHAR)
                else -> fail("Multiple matching items found")
            }
        }

        myFixture.checkResultByFile("$fileName.kt.after")
    }

    override fun getTestDataDirectory() = COMPLETION_TEST_DATA_BASE.resolve("handlers/multifile/smart")

    override fun defaultCompletionType(): CompletionType = CompletionType.BASIC
    override fun getPlatform(): TargetPlatform = JvmPlatforms.unspecifiedJvmPlatform
    override fun getProjectDescriptor() = LightJavaCodeInsightFixtureTestCase.JAVA_LATEST
}

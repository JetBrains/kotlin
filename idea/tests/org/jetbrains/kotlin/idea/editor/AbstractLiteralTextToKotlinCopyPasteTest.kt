/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.openapi.actionSystem.IdeActions
import org.jetbrains.kotlin.idea.AbstractCopyPasteTest
import org.jetbrains.kotlin.idea.caches.resolve.forceResolveInWriteActionCheckInTests
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractLiteralTextToKotlinCopyPasteTest : AbstractCopyPasteTest() {
    private val _basePath = PluginTestCaseBase.getTestDataPathBase() + "/copyPaste/plainTextLiteral"

    override fun getTestDataPath() = _basePath

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun doTest(path: String) {
        myFixture.testDataPath = _basePath
        val testName = getTestName(false)
        myFixture.configureByFiles("$testName.txt")
        val fileText = myFixture.editor.document.text

        if (!myFixture.editor.selectionModel.hasSelection())
            myFixture.editor.selectionModel.setSelection(0, fileText.length)

        forceResolveInWriteActionCheckInTests {
            myFixture.performEditorAction(IdeActions.ACTION_COPY)
        }

        configureTargetFile("$testName.kt")

        forceResolveInWriteActionCheckInTests {
            myFixture.performEditorAction(IdeActions.ACTION_PASTE)
        }

        KotlinTestUtils.assertEqualsToFile(File(path.replace(".txt", ".expected.kt")), myFixture.file.text)
    }
}

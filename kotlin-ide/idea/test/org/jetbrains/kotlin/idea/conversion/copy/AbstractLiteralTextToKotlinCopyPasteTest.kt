/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.openapi.actionSystem.IdeActions
import org.jetbrains.kotlin.idea.AbstractCopyPasteTest
import org.jetbrains.kotlin.idea.caches.resolve.forceCheckForResolveInDispatchThreadInTests
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractLiteralTextToKotlinCopyPasteTest : AbstractCopyPasteTest() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun doTest(unused: String) {
        val fileName = fileName()
        val targetFileName = fileName.replace(".txt", ".kt")

        myFixture.configureByFile(fileName)
        val fileText = myFixture.editor.document.text

        if (!myFixture.editor.selectionModel.hasSelection())
            myFixture.editor.selectionModel.setSelection(0, fileText.length)

        forceCheckForResolveInDispatchThreadInTests {
            myFixture.performEditorAction(IdeActions.ACTION_COPY)
        }

        configureTargetFile(targetFileName)

        forceCheckForResolveInDispatchThreadInTests {
            myFixture.performEditorAction(IdeActions.ACTION_PASTE)
        }

        val testFile = testDataFile()
        val expectedFile = File(testFile.parent, testFile.nameWithoutExtension + ".expected.kt")

        KotlinTestUtils.assertEqualsToFile(expectedFile, myFixture.file.text)
    }
}

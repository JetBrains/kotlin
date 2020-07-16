/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.openapi.actionSystem.IdeActions
import org.jetbrains.kotlin.idea.AbstractCopyPasteTest
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractLiteralKotlinToKotlinCopyPasteTest : AbstractCopyPasteTest() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun doTest(unused: String) {
        val fileName = fileName()

        val testFile = testDataFile()
        val expectedFile = File(testFile.parent, testFile.nameWithoutExtension + ".expected.kt")

        myFixture.configureByFile(fileName)

        myFixture.performEditorAction(IdeActions.ACTION_COPY)

        configureTargetFile(fileName.replace(".kt", ".to.kt"))

        myFixture.performEditorAction(IdeActions.ACTION_PASTE)
        KotlinTestUtils.assertEqualsToFile(expectedFile, myFixture.file.text)
    }
}

/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.openapi.actionSystem.IdeActions
import org.jetbrains.kotlin.idea.AbstractCopyPasteTest
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractLiteralKotlinToKotlinCopyPasteTest : AbstractCopyPasteTest() {
    private val BASE_PATH = PluginTestCaseBase.getTestDataPathBase() + "/copyPaste/literal"


    override fun getTestDataPath() = BASE_PATH

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE


    fun doTest(path: String) {
        myFixture.testDataPath = BASE_PATH
        val testName = getTestName(false)
        myFixture.configureByFiles(testName + ".kt")

        myFixture.performEditorAction(IdeActions.ACTION_COPY)

        configureTargetFile(testName + ".to.kt")

        myFixture.performEditorAction(IdeActions.ACTION_PASTE)
        KotlinTestUtils.assertEqualsToFile(File(path.replace(".kt", ".expected.kt")), myFixture.file.text)
    }
}

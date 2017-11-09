/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.openapi.actionSystem.IdeActions
import org.jetbrains.kotlin.idea.AbstractCopyPasteTest
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractLiteralTextToKotlinCopyPasteTest : AbstractCopyPasteTest() {
    private val BASE_PATH = PluginTestCaseBase.getTestDataPathBase() + "/copyPaste/plainTextLiteral"


    override fun getTestDataPath() = BASE_PATH

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE


    fun doTest(path: String) {
        myFixture.testDataPath = BASE_PATH
        val testName = getTestName(false)
        myFixture.configureByFiles(testName + ".txt")
        val fileText = myFixture.editor.document.text

        if (!myFixture.editor.selectionModel.hasSelection())
            myFixture.editor.selectionModel.setSelection(0, fileText.length)
        myFixture.performEditorAction(IdeActions.ACTION_COPY)

        configureTargetFile(testName + ".kt")

        myFixture.performEditorAction(IdeActions.ACTION_PASTE)
        KotlinTestUtils.assertEqualsToFile(File(path.replace(".txt", ".expected.kt")), myFixture.file.text)
    }
}

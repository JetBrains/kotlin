/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert

abstract class AbstractFunctionParameterInfoTest : LightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    override fun setUp() {
        super.setUp()
        myFixture.testDataPath = PluginTestCaseBase.getTestDataPathBase() + "/parameterInfo/functionParameterInfo"
    }

    protected fun doTest(fileName: String) {
        myFixture.configureByFile(fileName)

        val file = myFixture.file as JetFile

        val lastChild = file.allChildren.filter { it !is PsiWhiteSpace }.last()
        val expectedResultText = when (lastChild.node.elementType) {
            JetTokens.BLOCK_COMMENT -> lastChild.text.substring(2, lastChild.text.length() - 2).trim()
            JetTokens.EOL_COMMENT -> lastChild.text.substring(2).trim()
            else -> error("Unexpected last file child")
        }

        val parameterInfoHandler = KotlinFunctionParameterInfoHandler()
        val mockCreateParameterInfoContext = MockCreateParameterInfoContext(file, myFixture)
        val parameterOwner = parameterInfoHandler.findElementForParameterInfo(mockCreateParameterInfoContext)

        val textToType = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// TYPE:")
        if (textToType != null) {
            project.executeWriteCommand("") {
                val caretModel = myFixture.editor.caretModel
                val offset = caretModel.offset
                myFixture.getDocument(file).insertString(offset, textToType)
                caretModel.moveToOffset(offset + textToType.length())
            }
            PsiDocumentManager.getInstance(project).commitAllDocuments()
        }

        //to update current parameter index
        val updateContext = MockUpdateParameterInfoContext(file, myFixture)
        val elementForUpdating = parameterInfoHandler.findElementForUpdatingParameterInfo(updateContext)
        if (elementForUpdating != null) {
            parameterInfoHandler.updateParameterInfo(elementForUpdating, updateContext)
        }

        val parameterInfoUIContext = MockParameterInfoUIContext(parameterOwner, updateContext.currentParameter)

        for (item in mockCreateParameterInfoContext.itemsToShow) {
            //noinspection unchecked
            parameterInfoHandler.updateUI(item as FunctionDescriptor, parameterInfoUIContext)
        }
        Assert.assertEquals(expectedResultText, parameterInfoUIContext.resultText)
    }
}

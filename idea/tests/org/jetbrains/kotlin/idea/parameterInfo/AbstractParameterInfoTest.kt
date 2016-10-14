/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.codeInsight.hint.ShowParameterInfoContext
import com.intellij.codeInsight.hint.ShowParameterInfoHandler
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.test.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert

abstract class AbstractParameterInfoTest : LightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor {
        val root = KotlinTestUtils.getTestsRoot(this.javaClass)
        if (root.contains("Lib")) {
            return JdkAndMockLibraryProjectDescriptor(
                    "$root/sharedLib", true, true, false, false
            )

        }
        return ProjectDescriptorWithStdlibSources.INSTANCE
    }

    override fun setUp() {
        super.setUp()
        myFixture.testDataPath = PluginTestCaseBase.getTestDataPathBase() + "/parameterInfo"
    }

    protected fun doTest(fileName: String) {
        myFixture.configureByFile(fileName)

        val file = myFixture.file as KtFile

        val lastChild = file.allChildren.filter { it !is PsiWhiteSpace }.last()
        val expectedResultText = when (lastChild.node.elementType) {
            KtTokens.BLOCK_COMMENT -> lastChild.text.substring(2, lastChild.text.length - 2).trim()
            KtTokens.EOL_COMMENT -> lastChild.text.substring(2).trim()
            else -> error("Unexpected last file child")
        }

        val context = ShowParameterInfoContext(editor, project, file, editor.caretModel.offset, -1)

        val handlers = ShowParameterInfoHandler.getHandlers(project, KotlinLanguage.INSTANCE)!!
        val handler = handlers.firstOrNull { it.findElementForParameterInfo(context) != null }
            ?: error("Could not find parameter info handler")

        val mockCreateParameterInfoContext = MockCreateParameterInfoContext(file, myFixture)
        val parameterOwner = handler.findElementForParameterInfo(mockCreateParameterInfoContext) as PsiElement

        val textToType = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// TYPE:")
        if (textToType != null) {
            myFixture.type(textToType)
            PsiDocumentManager.getInstance(project).commitAllDocuments()
        }

        //to update current parameter index
        val updateContext = MockUpdateParameterInfoContext(file, myFixture)
        val elementForUpdating = handler.findElementForUpdatingParameterInfo(updateContext)
        if (elementForUpdating != null) {
            handler.updateParameterInfo(elementForUpdating, updateContext)
        }

        val parameterInfoUIContext = MockParameterInfoUIContext(parameterOwner, updateContext.currentParameter)

        for (item in mockCreateParameterInfoContext.itemsToShow) {
            handler.updateUI(item, parameterInfoUIContext)
        }
        Assert.assertEquals(expectedResultText, parameterInfoUIContext.resultText)
    }
}

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

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestUtil
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import kotlin.test.*
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.actionSystem.CommonDataKeys
import org.jetbrains.kotlin.idea.refactoring.rename.RenameKotlinImplicitLambdaParameter
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.command.WriteCommandAction
import org.jetbrains.kotlin.idea.refactoring.rename.findElementForRename
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class InplaceRenameTest : LightPlatformCodeInsightTestCase() {
    override fun isRunInWriteAction(): Boolean = false
    override fun getTestDataPath(): String = PluginTestCaseBase.getTestDataPathBase() + "/refactoring/rename/inplace/"

    fun testLocalVal() {
        doTestInplaceRename("y")
    }

    fun testForLoop() {
        doTestInplaceRename("j")
    }

    fun testTryCatch() {
        doTestInplaceRename("e1")
    }

    fun testFunctionLiteral() {
        doTestInplaceRename("y")
    }

    fun testFunctionLiteralIt() {
        doTestImplicitLambdaParameter("y")
    }

    fun testFunctionLiteralItEndCaret() {
        doTestImplicitLambdaParameter("y")
    }

    fun testFunctionLiteralParenthesis() {
        doTestInplaceRename("y")
    }

    fun testLocalFunction() {
        doTestInplaceRename("bar")
    }

    fun testFunctionParameterNotInplace() {
        doTestInplaceRename(null)
    }

    fun testGlobalFunctionNotInplace() {
        doTestInplaceRename(null)
    }

    fun testTopLevelValNotInplace() {
        doTestInplaceRename(null)
    }

    fun testLabelFromFunction() {
        doTestInplaceRename("foo")
    }

    fun testMultiDeclaration() {
        doTestInplaceRename("foo")
    }

    fun testLocalVarShadowingMemberProperty() {
        doTestInplaceRename("name1")
    }

    private fun doTestImplicitLambdaParameter(newName: String) {
        configureByFile(getTestName(false) + ".kt")

        // This code is copy-pasted from CodeInsightTestUtil.doInlineRename() and slightly modified.
        // Original method was not suitable because it expects renamed element to be reference to other or referrable

        val file = getFile()!!
        val editor = getEditor()!!
        val element = file.findElementForRename<KtNameReferenceExpression>(editor.caretModel.offset)!!
        assertNotNull(element)

        val dataContext = SimpleDataContext.getSimpleContext(CommonDataKeys.PSI_ELEMENT.name, element,
                                                             getCurrentEditorDataContext())
        val handler = RenameKotlinImplicitLambdaParameter()

        assertTrue(handler.isRenaming(dataContext), "In-place rename not allowed for " + element)

        val project = editor.project!!
        val templateManager = TemplateManager.getInstance(project) as TemplateManagerImpl
        try {
            templateManager.setTemplateTesting(true)

            object : WriteCommandAction.Simple<Any>(project) {
                override fun run() {
                    handler.invoke(project, editor, file, dataContext)
                }
            }.execute()

            var state = TemplateManagerImpl.getTemplateState(editor)
            assert(state != null)
            val range = state!!.currentVariableRange
            assert(range != null)
            object : WriteCommandAction.Simple<Any>(project) {
                override fun run() {
                    editor.document.replaceString(range!!.startOffset, range.endOffset, newName)
                }
            }.execute().throwException()

            state = TemplateManagerImpl.getTemplateState(editor)
            assert(state != null)
            state!!.gotoEnd(false)
        }
        finally {
            templateManager.setTemplateTesting(false)
        }


        checkResultByFile(getTestName(false) + ".kt.after")
    }

    private fun doTestInplaceRename(newName: String?) {
        configureByFile(getTestName(false) + ".kt")
        val element = TargetElementUtilBase.findTargetElement(
                LightPlatformCodeInsightTestCase.myEditor,
                TargetElementUtilBase.ELEMENT_NAME_ACCEPTED or TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED
        )

        assertNotNull(element)

        val dataContext = SimpleDataContext.getSimpleContext(CommonDataKeys.PSI_ELEMENT.name, element!!,
                                                             LightPlatformCodeInsightTestCase.getCurrentEditorDataContext())

        val handler = VariableInplaceRenameHandler()
        if (newName == null) {
            assertFalse(handler.isRenaming(dataContext), "In-place rename is allowed for " + element)
        }
        else {
            assertTrue(handler.isRenaming(dataContext), "In-place rename not allowed for " + element)
            CodeInsightTestUtil.doInlineRename(handler, newName, LightPlatformCodeInsightTestCase.getEditor(), element)
            checkResultByFile(getTestName(false) + ".kt.after")
        }
    }
}

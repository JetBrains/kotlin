/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestUtil
import org.jetbrains.kotlin.idea.refactoring.rename.RenameKotlinImplicitLambdaParameter
import org.jetbrains.kotlin.idea.refactoring.rename.findElementForRename
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        doTestMemberInplaceRename("bar")
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
        doTestMemberInplaceRename("foo")
    }

    fun testMultiDeclaration() {
        doTestInplaceRename("foo")
    }

    fun testLocalVarShadowingMemberProperty() {
        doTestInplaceRename("name1")
    }

    fun testNoReformat() {
        doTestMemberInplaceRename("subject2")
    }

    fun testInvokeToFoo() {
        doTestMemberInplaceRename("foo")
    }

    fun testInvokeToGet() {
        doTestMemberInplaceRename("get")
    }

    fun testInvokeToGetWithQualifiedExpr() {
        doTestMemberInplaceRename("get")
    }

    fun testInvokeToGetWithSafeQualifiedExpr() {
        doTestMemberInplaceRename("get")
    }

    fun testInvokeToPlus() {
        doTestMemberInplaceRename("plus")
    }

    fun testGetToFoo() {
        doTestMemberInplaceRename("foo")
    }

    fun testGetToInvoke() {
        doTestMemberInplaceRename("invoke")
    }

    fun testGetToInvokeWithQualifiedExpr() {
        doTestMemberInplaceRename("invoke")
    }

    fun testGetToInvokeWithSafeQualifiedExpr() {
        doTestMemberInplaceRename("invoke")
    }

    fun testGetToPlus() {
        doTestMemberInplaceRename("plus")
    }

    fun testAddQuotes() {
        doTestMemberInplaceRename("is")
    }

    fun testAddThis() {
        doTestMemberInplaceRename("foo")
    }

    fun testExtensionAndNoReceiver() {
        doTestMemberInplaceRename("b")
    }

    fun testTwoExtensions() {
        doTestMemberInplaceRename("example")
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

    private fun doTestMemberInplaceRename(newName: String?) {
        doTestInplaceRename(newName, MemberInplaceRenameHandler())
    }

    private fun doTestInplaceRename(newName: String?, handler: VariableInplaceRenameHandler = VariableInplaceRenameHandler()) {
        configureByFile(getTestName(false) + ".kt")
        val element = TargetElementUtil.findTargetElement(
                LightPlatformCodeInsightTestCase.myEditor,
                TargetElementUtil.ELEMENT_NAME_ACCEPTED or TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED
        )

        assertNotNull(element)

        val dataContext = SimpleDataContext.getSimpleContext(CommonDataKeys.PSI_ELEMENT.name, element!!,
                                                             LightPlatformCodeInsightTestCase.getCurrentEditorDataContext())

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

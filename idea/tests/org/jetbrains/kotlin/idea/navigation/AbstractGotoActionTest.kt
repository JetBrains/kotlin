/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigation

import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert

abstract class AbstractGotoActionTest : KotlinLightCodeInsightFixtureTestCase() {
    protected abstract val actionName: String

    protected fun doTest(testPath: String) {
        val parts = KotlinTestUtils.loadBeforeAfterText(testPath)

        myFixture.configureByText(KotlinFileType.INSTANCE, parts[0])

        val gotoAction = ActionManager.getInstance().getAction(actionName) as CodeInsightAction
        gotoAction.actionPerformedImpl(project, myFixture.editor)

        val fileEditorManager = FileEditorManager.getInstance(myFixture.project) as FileEditorManagerEx
        val currentEditor = fileEditorManager.selectedTextEditor ?: editor

        if (currentEditor == editor) {
            val text = myFixture.getDocument(myFixture.file).text
            val afterText = StringBuilder(text).insert(editor.caretModel.offset, "<caret>").toString()

            Assert.assertEquals(parts[1], afterText)
        } else {
            val fileOffset = currentEditor.caretModel.offset
            val lineNumber = currentEditor.document.getLineNumber(fileOffset)
            val lineStart = currentEditor.document.getLineStartOffset(lineNumber)
            val lineEnd = currentEditor.document.getLineEndOffset(lineNumber)
            val inLineOffset = fileOffset - lineStart

            val line = currentEditor.document.getText(TextRange(lineStart, lineEnd))
            val withCaret = with(StringBuilder()) {
                append(line)
                insert(inLineOffset, "<caret>")
                toString()
            }

            Assert.assertEquals(parts[1], withCaret)
        }
    }

    override fun getProjectDescriptor() = getProjectDescriptorFromTestName()
}

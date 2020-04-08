/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.suggested

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Parameter
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Signature
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import com.intellij.refactoring.suggested.BaseSuggestedRefactoringChangeCollectorTest
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.runTest

class KotlinSuggestedRefactoringChangeCollectorTest : BaseSuggestedRefactoringChangeCollectorTest<KtNamedFunction>() {
    override val fileType: FileType
        get() = KotlinFileType.INSTANCE

    override val language: Language
        get() = KotlinLanguage.INSTANCE

    override fun addDeclaration(file: PsiFile, text: String): KtNamedFunction {
        val psiFactory = KtPsiFactory(project)
        return (file as KtFile).add(psiFactory.createDeclaration(text)) as KtNamedFunction
    }

    override fun Signature.presentation(labelForParameterId: (Any) -> String?): String {
        return buildString {
            append("fun ")
            val receiverType = (additionalData as KotlinSignatureAdditionalData?)?.receiverType
            if (receiverType != null) {
                append(receiverType)
                append(".")
            }
            append(name)
            append("(")
            parameters.joinTo(this, separator = ", ") { it.presentation(labelForParameterId(it.id)) }
            append(")")
            if (type != null) {
                append(": ")
                append(type)
            }
        }
    }

    private fun Parameter.presentation(label: String?): String {
        return buildString {
            if (modifiers.isNotEmpty()) {
                append(modifiers)
                append(" ")
            }
            append(name)
            append(": ")
            append(type)
            if (label != null) {
                append(" (")
                append(label)
                append(")")
            }
        }
    }

    private fun createType(text: String): KtTypeReference {
        return KtPsiFactory(project).createType(text)
    } 
    
    private fun createParameter(text: String): KtParameter {
        return KtPsiFactory(project).createParameter(text)
    }
    
    fun testAddParameter() {
        doTest(
            "fun foo(p1: Int) {}",
            { it.valueParameterList!!.addParameter(createParameter("p2: Int")) },
            expectedOldSignature = "fun foo(p1: Int)",
            expectedNewSignature = "fun foo(p1: Int (initialIndex = 0), p2: Int (new))"
        )
    }

    fun testRemoveParameter() {
        doTest(
            "fun foo(p1: Int, p2: Int) {}",
            { it.valueParameterList!!.removeParameter(0) },
            expectedOldSignature = "fun foo(p1: Int, p2: Int)",
            expectedNewSignature = "fun foo(p2: Int (initialIndex = 1))"
        )
    }

    fun testChangeParameterType() {
        doTest(
            "fun foo(p1: Int, p2: Int) {}",
            { it.valueParameters[1].typeReference = createType("Any?") },
            expectedOldSignature = "fun foo(p1: Int, p2: Int)",
            expectedNewSignature = "fun foo(p1: Int (initialIndex = 0), p2: Any? (initialIndex = 1))"
        )
    }

    fun testChangeParameterNames() {
        doTest(
            "fun foo(p1: Int, p2: Int) {}",
            { it.valueParameters[0].setName("newP1") },
            { it.valueParameters[1].setName("newP2") },
            expectedOldSignature = "fun foo(p1: Int, p2: Int)",
            expectedNewSignature = "fun foo(newP1: Int (initialIndex = 0), newP2: Int (initialIndex = 1))"
        )
    }

    fun testReplaceParameter() {
        doTest(
            "fun foo(p1: Int, p2: Int) {}",
            { it.valueParameters[0].replace(createParameter("newP1: Long")) },
            expectedOldSignature = "fun foo(p1: Int, p2: Int)",
            expectedNewSignature = "fun foo(newP1: Long (new), p2: Int (initialIndex = 1))"
        )
    }

    fun testReorderParametersChangeTypesAndNames() {
        doTest(
            "fun foo(p1: Int, p2: Int, p3: Int) {}",
            {
                editor.caretModel.moveToOffset(it.valueParameters[2].textOffset)
                myFixture.performEditorAction(IdeActions.MOVE_ELEMENT_LEFT)
                myFixture.performEditorAction(IdeActions.MOVE_ELEMENT_LEFT)
            },
            {
                executeCommand {
                    runWriteAction {
                        it.valueParameters[0].typeReference = createType("Any?")
                        it.valueParameters[1].typeReference = createType("Long")
                        it.valueParameters[2].typeReference = createType("Double")
                    }
                }
            },
            {
                executeCommand {
                    runWriteAction {
                        it.valueParameters[1].setName("newName")
                    }
                }
            },
            wrapIntoCommandAndWriteAction = false,
            expectedOldSignature = "fun foo(p1: Int, p2: Int, p3: Int)",
            expectedNewSignature = "fun foo(p3: Any? (initialIndex = 2), newName: Long (initialIndex = 0), p2: Double (initialIndex = 1))"
        )
    }

    fun testReorderParametersByCutPaste() {
        doTest(
            "fun foo(p1: Int, p2: String, p3: Char)",
            {
                val offset = it.valueParameters[1].textRange.endOffset
                editor.caretModel.moveToOffset(offset)
                editor.selectionModel.setSelection(offset, offset + ", p3: Char".length)
                myFixture.performEditorAction(IdeActions.ACTION_EDITOR_CUT)
            },
            {
                val offset = it.valueParameters[0].textRange.endOffset
                editor.caretModel.moveToOffset(offset)
                myFixture.performEditorAction(IdeActions.ACTION_EDITOR_PASTE)
            },
            wrapIntoCommandAndWriteAction = false,
            expectedOldSignature = "fun foo(p1: Int, p2: String, p3: Char)",
            expectedNewSignature = "fun foo(p1: Int (initialIndex = 0), p3: Char (initialIndex = 2), p2: String (initialIndex = 1))"
        )
    }

    fun testReorderParametersByCutPasteAfterChangingName() {
        doTest(
            "fun foo(p1: Int, p2: String, p3: Char)",
            {
                executeCommand {
                    runWriteAction {
                        it.valueParameters[2].setName("p3New")
                        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
                    }
                }
            },
            {
                val offset = it.valueParameters[1].textRange.endOffset
                editor.caretModel.moveToOffset(offset)
                editor.selectionModel.setSelection(offset, offset + ", p3New: Char".length)
                myFixture.performEditorAction(IdeActions.ACTION_EDITOR_CUT)
            },
            {
                val offset = it.valueParameters[0].textRange.endOffset
                editor.caretModel.moveToOffset(offset)
                myFixture.performEditorAction(IdeActions.ACTION_EDITOR_PASTE)
            },
            wrapIntoCommandAndWriteAction = false,
            expectedOldSignature = "fun foo(p1: Int, p2: String, p3: Char)",
            expectedNewSignature = "fun foo(p1: Int (initialIndex = 0), p3New: Char (initialIndex = 2), p2: String (initialIndex = 1))"
        )
    }

    override fun runTest() {
        runTest { super.runTest() }
    }
}

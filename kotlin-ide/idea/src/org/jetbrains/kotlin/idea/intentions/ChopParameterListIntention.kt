/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.formatter.kotlinCommonSettings
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

abstract class AbstractChopListIntention<TList : KtElement, TElement : KtElement>(
    listClass: Class<TList>,
    private val elementClass: Class<TElement>,
    textGetter: () -> String
) : SelfTargetingIntention<TList>(listClass, textGetter) {
    override fun allowCaretInsideElement(element: PsiElement) =
        element !is KtValueArgument && super.allowCaretInsideElement(element)

    open fun leftParOnNewLine(commonCodeStyleSettings: CommonCodeStyleSettings): Boolean = false

    open fun rightParOnNewLine(commonCodeStyleSettings: CommonCodeStyleSettings): Boolean = false

    override fun isApplicableTo(element: TList, caretOffset: Int): Boolean {
        val elements = element.elements()
        if (elements.size <= 1) return false
        if (!isApplicableCaretOffset(caretOffset, element)) return false
        if (elements.dropLast(1).all { hasLineBreakAfter(it) }) return false
        return true
    }

    override fun applyTo(element: TList, editor: Editor?) {
        val project = element.project
        val document = editor?.document ?: return
        val pointer = element.createSmartPointer()

        val commonCodeStyleSettings = CodeStyle.getSettings(project).kotlinCommonSettings
        val leftParOnNewLine = leftParOnNewLine(commonCodeStyleSettings)
        val rightParOnNewLine = rightParOnNewLine(commonCodeStyleSettings)

        val elements = element.elements()
        if (rightParOnNewLine && !hasLineBreakAfter(elements.last())) {
            element.allChildren.lastOrNull { it.node.elementType == KtTokens.RPAR }?.startOffset?.let { document.insertString(it, "\n") }
        }

        val maxIndex = elements.size - 1
        for ((index, e) in elements.asReversed().withIndex()) {
            if (index == maxIndex && !leftParOnNewLine) break
            if (!hasLineBreakBefore(e)) {
                document.insertString(e.startOffset, "\n")
            }
        }

        val documentManager = PsiDocumentManager.getInstance(project)
        documentManager.commitDocument(document)
        pointer.element?.let { CodeStyleManager.getInstance(project).reformat(it) }
    }

    protected fun hasLineBreakAfter(element: TElement): Boolean = nextBreak(element) != null

    protected fun nextBreak(element: TElement): PsiWhiteSpace? = element.siblings(withItself = false)
        .takeWhile { !elementClass.isInstance(it) }
        .firstOrNull { it is PsiWhiteSpace && it.textContains('\n') } as? PsiWhiteSpace

    protected fun hasLineBreakBefore(element: TElement): Boolean = prevBreak(element) != null

    protected fun prevBreak(element: TElement): PsiWhiteSpace? = element.siblings(withItself = false, forward = false)
        .takeWhile { !elementClass.isInstance(it) }
        .firstOrNull { it is PsiWhiteSpace && it.textContains('\n') } as? PsiWhiteSpace

    protected fun TList.elements(): List<TElement> = allChildren.filter { elementClass.isInstance(it) }
        .map {
            @Suppress("UNCHECKED_CAST")
            it as TElement
        }
        .toList()

    protected fun isApplicableCaretOffset(caretOffset: Int, element: TList): Boolean {
        val elementBeforeCaret = element.containingFile.findElementAt(caretOffset - 1) ?: return true
        if (elementBeforeCaret.node.elementType != KtTokens.RPAR) return true
        return elementBeforeCaret.parent == element
    }
}

class ChopParameterListIntention : AbstractChopListIntention<KtParameterList, KtParameter>(
    KtParameterList::class.java,
    KtParameter::class.java,
    KotlinBundle.lazyMessage("put.parameters.on.separate.lines")
) {
    override fun isApplicableTo(element: KtParameterList, caretOffset: Int): Boolean {
        if (element.parent is KtFunctionLiteral) return false
        return super.isApplicableTo(element, caretOffset)
    }

    override fun leftParOnNewLine(commonCodeStyleSettings: CommonCodeStyleSettings): Boolean {
        return commonCodeStyleSettings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE
    }

    override fun rightParOnNewLine(commonCodeStyleSettings: CommonCodeStyleSettings): Boolean {
        return commonCodeStyleSettings.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE
    }
}

class ChopArgumentListIntention : AbstractChopListIntention<KtValueArgumentList, KtValueArgument>(
    KtValueArgumentList::class.java,
    KtValueArgument::class.java,
    KotlinBundle.lazyMessage("put.arguments.on.separate.lines")
) {
    override fun leftParOnNewLine(commonCodeStyleSettings: CommonCodeStyleSettings): Boolean {
        return commonCodeStyleSettings.CALL_PARAMETERS_LPAREN_ON_NEXT_LINE
    }

    override fun rightParOnNewLine(commonCodeStyleSettings: CommonCodeStyleSettings): Boolean {
        return commonCodeStyleSettings.CALL_PARAMETERS_RPAREN_ON_NEXT_LINE
    }
}

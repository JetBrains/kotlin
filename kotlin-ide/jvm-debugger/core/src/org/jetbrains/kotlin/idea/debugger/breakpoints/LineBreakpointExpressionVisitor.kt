/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.breakpoints

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class LineBreakpointExpressionVisitor private constructor(
    private val document: Document,
    private val mainLine: Int
) : KtVisitor<ApplicabilityResult, Unit?>() {
    companion object {
        @JvmStatic
        fun of(file: VirtualFile, line: Int): LineBreakpointExpressionVisitor? {
            val document = FileDocumentManager.getInstance().getDocument(file) ?: return null
            return LineBreakpointExpressionVisitor(document, line)
        }
    }

    override fun visitKtElement(element: KtElement, data: Unit?): ApplicabilityResult {
        return visitChildren(element.children.asList(), data)
    }

    private fun visitChildren(children: List<PsiElement>, data: Unit?): ApplicabilityResult {
        if (children.isEmpty() || !children.first().isSameLine()) {
            return ApplicabilityResult.UNKNOWN
        }

        var isApplicable = false

        for (child in children) {
            val ktElement = child as? KtElement ?: continue
            val result = ktElement.accept(this, data)
            if (result.shouldStop) {
                return result
            }

            isApplicable = isApplicable or result.isApplicable
        }

        return ApplicabilityResult.maybe(isApplicable)
    }

    override fun visitExpression(expression: KtExpression, data: Unit?): ApplicabilityResult {
        if (expression.isSameLine()) {
            val superResult = super.visitExpression(expression, data)
            if (superResult.shouldStop) {
                return superResult
            }

            return ApplicabilityResult.MAYBE_YES
        }

        return ApplicabilityResult.UNKNOWN
    }

    override fun visitClass(klass: KtClass, data: Unit?): ApplicabilityResult {
        visitChildren(klass.primaryConstructorParameters, data).handle()?.let { return it }
        return klass.body?.accept(this, data) ?: ApplicabilityResult.UNKNOWN
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration, data: Unit?): ApplicabilityResult {
        return declaration.body?.accept(this, data) ?: ApplicabilityResult.UNKNOWN
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: Unit?): ApplicabilityResult {
        return constructor.bodyExpression?.accept(this, data)
            ?.acceptIfMultiLineParent(constructor)
            ?: ApplicabilityResult.UNKNOWN
    }

    override fun visitParameter(parameter: KtParameter, data: Unit?): ApplicabilityResult {
        val defaultValue = parameter.defaultValue
        if (defaultValue.isSameLine()) {
            return ApplicabilityResult.DEFINITELY_YES
        }

        return ApplicabilityResult.UNKNOWN
    }

    override fun visitCatchSection(catchClause: KtCatchClause, data: Unit?): ApplicabilityResult {
        return ApplicabilityResult.MAYBE_YES
    }

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: Unit?): ApplicabilityResult {
        return ApplicabilityResult.UNKNOWN
    }

    override fun visitPackageDirective(directive: KtPackageDirective, data: Unit?): ApplicabilityResult {
        return ApplicabilityResult.UNKNOWN
    }

    override fun visitImportDirective(importDirective: KtImportDirective, data: Unit?): ApplicabilityResult {
        return ApplicabilityResult.UNKNOWN
    }

    override fun visitProperty(property: KtProperty, data: Unit?): ApplicabilityResult {
        if (property.hasModifier(KtTokens.CONST_KEYWORD)) {
            return ApplicabilityResult.UNKNOWN
        }

        return super.visitProperty(property, data)
    }

    override fun visitNamedFunction(function: KtNamedFunction, data: Unit?): ApplicabilityResult {
        if (function.isSameLine() && KtPsiUtil.isLocal(function)) {
            return ApplicabilityResult.MAYBE_YES
        }

        visitChildren(function.valueParameters, data).handle()?.let { return it }

        return function.bodyExpression?.accept(this, data)
            ?.acceptIfMultiLineParent(function)
            ?: ApplicabilityResult.UNKNOWN
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor, data: Unit?): ApplicabilityResult {
        return accessor.bodyExpression?.accept(this, data)
            ?.acceptIfMultiLineParent(accessor)
            ?: ApplicabilityResult.UNKNOWN
    }

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate, data: Unit?): ApplicabilityResult {
        return ApplicabilityResult.maybe(delegate.isSameLine())
    }

    override fun visitLambdaExpression(expression: KtLambdaExpression, data: Unit?): ApplicabilityResult {
        if (expression.isSameLine()) {
            return ApplicabilityResult.DEFINITELY_YES
        }

        return ApplicabilityResult.UNKNOWN
    }

    override fun visitWhenEntry(jetWhenEntry: KtWhenEntry, data: Unit?): ApplicabilityResult {
        jetWhenEntry.expression?.accept(this, data)?.handle()?.let { return it }
        return ApplicabilityResult.maybe(jetWhenEntry.conditions.isNotEmpty() || jetWhenEntry.isElse)
    }

    override fun visitDestructuringDeclaration(multiDeclaration: KtDestructuringDeclaration, data: Unit?): ApplicabilityResult {
        return ApplicabilityResult.maybe(multiDeclaration.isSameLine())
    }

    override fun visitBlockExpression(expression: KtBlockExpression, data: Unit?): ApplicabilityResult {
        return visitChildren(expression.statements, data)
    }

    override fun visitParenthesizedExpression(expression: KtParenthesizedExpression, data: Unit?): ApplicabilityResult {
        val parenthesized = expression.expression ?: return ApplicabilityResult.UNKNOWN
        val lines = parenthesized.getLines()

        if (lines.start > mainLine) {
            return ApplicabilityResult.UNKNOWN
        }

        if (lines.isMultiLine) {
            return parenthesized.accept(this, data)
        }

        return ApplicabilityResult.UNKNOWN
    }

    private fun PsiElement?.isSameLine(): Boolean {
        val lines = getLines()
        return lines.start == mainLine
    }

    private fun PsiElement?.getLines(): Lines {
        if (this == null) {
            return Lines.EMPTY
        }

        val startOffset = maxOf(this.startOffset, this.textOffset)
        val endOffset = this.endOffset

        if (startOffset < 0 || endOffset < 0 || startOffset > endOffset) {
            return Lines.EMPTY
        }

        val maxOffset = document.textLength
        if (startOffset > maxOffset || endOffset > maxOffset) {
            return Lines.EMPTY
        }

        val startLine = document.getLineNumber(startOffset)
        val endLine = document.getLineNumber(endOffset)
        return Lines(startLine, endLine)
    }

    private fun ApplicabilityResult.acceptIfMultiLineParent(parent: KtExpression): ApplicabilityResult? {
        if (this.shouldStop) {
            return this
        }

        if (parent.getLines().isSingleLine) {
            return null
        }

        return this
    }
}

private fun ApplicabilityResult.handle(): ApplicabilityResult? {
    if (this.shouldStop || this.isApplicable) {
        return this
    }

    return null
}

private class Lines(val start: Int, val end: Int) {
    companion object {
        val EMPTY = Lines(-1, -1)
    }

    val isSingleLine: Boolean
        get() = start == end && start >= 0

    val isMultiLine: Boolean
        get() = start != end && start >= 0 && end >= 0
}
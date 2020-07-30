/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.codeInliner.CallableUsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.unwrapSpecialUsageOrNull
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.j2k.j2kText
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.types.typeUtil.isUnit

class JavaToKotlinInlineHandler : AbstractCrossLanguageInlineHandler() {
    override fun prepareReference(reference: PsiReference, referenced: PsiElement): MultiMap<PsiElement, String> {
        return super.prepareReference(reference, referenced)
    }

    override fun performInline(usage: UsageInfo, referenced: PsiElement) {
        val unwrappedElement = unwrapUsage(usage) ?: error("Element from $usage not found")
        val j2kText = referenced.j2kText() ?: error("can't create")
        val declaration =
            KtPsiFactory(unwrappedElement).createExpressionCodeFragment(j2kText, unwrappedElement).getContentElement() as KtNamedFunction
        val returnType = declaration.resolveType()
        val codeToInline = buildCodeToInline(
            declaration,
            returnType,
            declaration.hasDeclaredReturnType() || (declaration.hasBlockBody() && returnType.isUnit()),
            declaration.bodyExpression!!,
            declaration.hasBlockBody(),
            referenced.findExistingEditor()
        ) ?: error("Can't create codeToInline")
        val replacementStrategy = CallableUsageReplacementStrategy(codeToInline, inlineSetter = false)
        replacementStrategy.createReplacer(unwrappedElement)?.invoke()
    }
}

private fun unwrapUsage(usage: UsageInfo): KtReferenceExpression? {
    val ktReferenceExpression = usage.element as? KtReferenceExpression ?: return null
    return unwrapSpecialUsageOrNull(ktReferenceExpression) ?: ktReferenceExpression
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode


fun KtCallElement.replaceOrCreateTypeArgumentList(newTypeArgumentList: KtTypeArgumentList) {
    if (typeArgumentList != null) typeArgumentList?.replace(newTypeArgumentList)
    else addAfter(
        newTypeArgumentList,
        calleeExpression,
    )
}

fun KtModifierListOwner.hasInlineModifier() = hasModifier(KtTokens.INLINE_KEYWORD)

fun KtModifierListOwner.hasPrivateModifier() = hasModifier(KtTokens.PRIVATE_KEYWORD)

fun KtPrimaryConstructor.mustHaveValOrVar(): Boolean = containingClass()?.let {
    it.isAnnotation() || it.hasInlineModifier()
} ?: false

// TODO: add cases
fun KtExpression.hasNoSideEffects(): Boolean = when (this) {
    is KtStringTemplateExpression -> !hasInterpolation()
    is KtConstantExpression -> true
    else -> ConstantExpressionEvaluator.getConstant(this, analyze(BodyResolveMode.PARTIAL)) != null
}

fun PsiElement.textRangeIn(other: PsiElement): TextRange = textRange.shiftLeft(other.startOffset)

fun KtDotQualifiedExpression.calleeTextRangeInThis(): TextRange? = callExpression?.calleeExpression?.textRangeIn(this)

fun KtNamedDeclaration.nameIdentifierTextRangeInThis(): TextRange? = nameIdentifier?.textRangeIn(this)

fun PsiElement.hasComments(): Boolean = anyDescendantOfType<PsiComment>()

fun KtDotQualifiedExpression.hasNotReceiver(): Boolean {
    val element = getQualifiedElementSelector()?.mainReference?.resolve() ?: return false
    return element is KtClassOrObject ||
            element is KtConstructor<*> ||
            element is KtCallableDeclaration && element.receiverTypeReference == null && (element.containingClassOrObject is KtObjectDeclaration?) ||
            element is PsiMember && element.hasModifier(JvmModifier.STATIC) ||
            element is PsiMethod && element.isConstructor
}

val KtExpression.isUnitLiteral: Boolean
    get() = KotlinBuiltIns.FQ_NAMES.unit.shortName() == (this as? KtNameReferenceExpression)?.getReferencedNameAsName()

val PsiElement.isAnonymousFunction: Boolean get() = this is KtNamedFunction && isAnonymousFunction

val KtNamedFunction.isAnonymousFunction: Boolean get() = nameIdentifier == null

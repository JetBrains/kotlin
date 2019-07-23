/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.j2k.getContainingClass
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.asQualifierWithThisAsSelector
import org.jetbrains.kotlin.nj2k.copyTreeAndDetach
import org.jetbrains.kotlin.nj2k.parentOfType
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class StaticMemberAccessConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        val symbol =
            when (element) {
                is JKFieldAccessExpression -> element.identifier
                is JKMethodCallExpression -> element.identifier
                else -> null
            } ?: return recurse(element)
        if (element.asQualifierWithThisAsSelector() != null) return recurse(element)
        if (symbol.isStaticMember()) {
            val containingClassSymbol = symbol.containingClassSymbol() ?: return recurse(element)
            return recurse(
                JKQualifiedExpressionImpl(
                    JKClassAccessExpressionImpl(containingClassSymbol),
                    JKKtQualifierImpl.DOT,
                    element.copyTreeAndDetach() as JKExpression
                )
            )
        }
        return recurse(element)

    }

    private fun JKSymbol.isStaticMember(): Boolean {
        val target = target
        return when (target) {
            is PsiModifierListOwner -> target.hasModifier(JvmModifier.STATIC)
            is KtElement -> target.parentOfType<KtClassOrObject>()
                ?.safeAs<KtObjectDeclaration>()
                ?.isCompanion() == true
            is JKTreeElement ->
                target.safeAs<JKOtherModifiersOwner>()?.hasOtherModifier(OtherModifier.STATIC) == true
                        || target.parentOfType<JKClass>()?.classKind == JKClass.ClassKind.OBJECT
            else -> false
        }
    }

    private fun JKSymbol.containingClassSymbol(): JKClassSymbol? {
        val target = target
        return when (target) {
            is KtElement ->
                target.parentOfType<KtClassOrObject>()?.let { klass ->
                    if (klass is KtObjectDeclaration && klass.isCompanion()) klass.containingClass()
                    else klass
                }?.let { context.symbolProvider.provideDirectSymbol(it) }

            is PsiElement -> target.getContainingClass()?.let {
                context.symbolProvider.provideDirectSymbol(it)
            }

            is JKTreeElement ->
                target.parentOfType<JKClass>()?.let { klass ->
                    if (klass.classKind == JKClass.ClassKind.COMPANION) klass.parentOfType<JKClass>()
                    else klass
                }?.let { context.symbolProvider.provideUniverseSymbol(it) }
            else -> error("bad isStaticMember")
        } as JKClassSymbol?
    }

}


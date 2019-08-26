/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.PsiEnumConstant
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.symbols.*
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKClassAccessExpressionImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKFieldAccessExpressionImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtQualifierImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKQualifiedExpressionImpl
import org.jetbrains.kotlin.psi.psiUtil.containingClass


class EnumFieldAccessConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKFieldAccessExpression) return recurse(element)
        if ((element.parent as? JKQualifiedExpression)?.selector == element) return recurse(element)
        val enumsClassSymbol = element.identifier.enumClassSymbol() ?: return recurse(element)

        return recurse(
            JKQualifiedExpressionImpl(
                JKClassAccessExpressionImpl(enumsClassSymbol),
                JKKtQualifierImpl.DOT,
                JKFieldAccessExpressionImpl(element.identifier)
            )
        )
    }

    private fun JKFieldSymbol.enumClassSymbol(): JKClassSymbol? =
        when {
            this is JKMultiverseFieldSymbol && target is PsiEnumConstant ->
                context.symbolProvider.provideDirectSymbol(target.containingClass!!) as JKClassSymbol
            this is JKMultiverseKtEnumEntrySymbol ->
                context.symbolProvider.provideDirectSymbol(target.containingClass()!!) as JKClassSymbol
            this is JKUniverseFieldSymbol && target is JKEnumConstant ->
                context.symbolProvider.provideUniverseSymbol(target.parentOfType<JKClass>()!!)
            else -> null
        }
}
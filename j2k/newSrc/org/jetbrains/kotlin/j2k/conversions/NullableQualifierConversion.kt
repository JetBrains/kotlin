/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.bangedBangedExpr
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*

class NullableQualifierConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKQualifiedExpression) return recurse(element)
        if (element.isSystemCall()) return recurse(element)// we don't want to have `System.err!!.println()` like expressions :)
        if (element.receiver.type(context)?.nullability in listOf(Nullability.Nullable, Nullability.Default)
            && element.operator == JKJavaQualifierImpl.DOT || element.operator == JKKtQualifierImpl.DOT
        ) {
            return recurse(
                JKQualifiedExpressionImpl(
                    element::receiver.detached().bangedBangedExpr(context),
                    element.operator,
                    element::selector.detached()
                )
            )
        }
        return recurse(element)
    }

    private fun JKQualifiedExpression.isSystemCall(): Boolean {
        val deepestQualifiedExpression = generateSequence(this) { expression ->
            expression.receiver as? JKQualifiedExpression
        }.last()
        return (deepestQualifiedExpression.receiver as? JKClassAccessExpression)
            ?.identifier
            ?.fqName
            ?.startsWith("java.") == true
    }
}
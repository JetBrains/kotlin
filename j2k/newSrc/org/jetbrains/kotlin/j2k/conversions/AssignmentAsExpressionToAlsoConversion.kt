/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.name.ClassId

class AssignmentAsExpressionToAlsoConversion(val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaAssignmentExpression) return recurse(element)

        val alsoSymbol = context.symbolProvider.provideDirectSymbol(
            resolveFqName(ClassId.fromString("kotlin/also"), element.getParentOfType<JKClass>() ?: element, context)!!
        ) as JKMethodSymbol

        val alsoExpression = JKKtAlsoCallExpressionImpl(
            JKKtAssignmentStatementImpl(element.field, JKStubExpressionImpl(), element.operator), alsoSymbol
        ).also {
            (it.statement as JKKtAssignmentStatement).expression =
                    JKKtFieldAccessExpressionImpl(context.symbolProvider.provideUniverseSymbol(it.parameter))
        }
        element.invalidate()

        return JKQualifiedExpressionImpl(
            element.expression,
            JKKtQualifierImpl.DOT,
            alsoExpression
        )

    }
}
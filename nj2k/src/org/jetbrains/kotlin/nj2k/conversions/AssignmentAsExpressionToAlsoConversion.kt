/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.utils.addToStdlib.cast

class AssignmentAsExpressionToAlsoConversion(val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaAssignmentExpression) return recurse(element)
        element.invalidate()

        val alsoExpression = JKKtAlsoCallExpressionImpl(
            JKBlockStatementImpl(
                JKBlockImpl(listOf(JKKtAssignmentStatementImpl(element.field, JKStubExpressionImpl(), element.operator)))
            ), context.symbolProvider.provideByFqName("kotlin/also")
        ).also {
            it.statement.cast<JKBlockStatement>().block.statements.first().cast<JKKtAssignmentStatement>().expression =
                    JKFieldAccessExpressionImpl(
                        context.symbolProvider.provideUniverseSymbol(
                            JKParameterImpl(JKTypeElementImpl(JKContextType), JKNameIdentifierImpl("it"))
                        )
                    )//TODO introduce symbol
        }

        return recurse(
            JKQualifiedExpressionImpl(
                element.expression,
                JKKtQualifierImpl.DOT,
                alsoExpression
            )
        )
    }
}
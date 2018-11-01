/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.JKExpression
import org.jetbrains.kotlin.j2k.tree.JKKtOperatorExpression
import org.jetbrains.kotlin.j2k.tree.impl.JKKtOperatorExpressionImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKMethodSymbol
import org.jetbrains.kotlin.name.ClassId

fun untilToExpression(
    from: JKExpression,
    to: JKExpression,
    conversionContext: ConversionContext,
    psiContext: PsiElement
): JKKtOperatorExpression =
    rangeExpression(
        from,
        to,
        "until",
        conversionContext,
        psiContext
    )

fun downToExpression(
    from: JKExpression,
    to: JKExpression,
    conversionContext: ConversionContext,
    psiContext: PsiElement
): JKKtOperatorExpression =
    rangeExpression(
        from,
        to,
        "downTo",
        conversionContext,
        psiContext
    )

fun rangeExpression(
    from: JKExpression,
    to: JKExpression,
    operatorName: String,
    conversionContext: ConversionContext,
    psiContext: PsiElement
): JKKtOperatorExpressionImpl {
    val symbol = conversionContext.symbolProvider.provideDirectSymbol(
        multiResolveFqName(ClassId.fromString("kotlin/ranges/$operatorName"), psiContext).first()
    ) as JKMethodSymbol
    return JKKtOperatorExpressionImpl(from, symbol, to)
}
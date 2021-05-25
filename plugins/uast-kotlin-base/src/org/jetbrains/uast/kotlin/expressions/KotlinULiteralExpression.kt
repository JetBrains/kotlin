/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.kotlin.internal.KotlinFakeUElement
import org.jetbrains.uast.wrapULiteral

class KotlinULiteralExpression(
    override val sourcePsi: KtConstantExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), ULiteralExpression, KotlinUElementWithType, KotlinEvaluatableUElement, KotlinFakeUElement {
    override val isNull: Boolean
        get() = sourcePsi.unwrapBlockOrParenthesis().node?.elementType == KtNodeTypes.NULL

    override val value by lz { evaluate() }

    override fun unwrapToSourcePsi(): List<PsiElement> = listOfNotNull(wrapULiteral(this).sourcePsi)
}

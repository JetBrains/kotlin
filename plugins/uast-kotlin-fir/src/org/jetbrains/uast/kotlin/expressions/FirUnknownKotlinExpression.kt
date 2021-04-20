/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.kotlin.FirKotlinAbstractUExpression

class FirUnknownKotlinExpression(
    override val sourcePsi: KtExpression,
    givenParent: UElement?
) : FirKotlinAbstractUExpression(givenParent), UExpression {
    override fun asLogString() = "[!] FirUnknownKotlinExpression ($sourcePsi)"
}

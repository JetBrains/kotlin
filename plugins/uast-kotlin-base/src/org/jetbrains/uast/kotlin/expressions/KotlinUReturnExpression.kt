/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UReturnExpression

class KotlinUReturnExpression(
    override val sourcePsi: KtReturnExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UReturnExpression, KotlinUElementWithType {
    override val returnExpression by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrNull(sourcePsi.returnedExpression, this)
    }
}

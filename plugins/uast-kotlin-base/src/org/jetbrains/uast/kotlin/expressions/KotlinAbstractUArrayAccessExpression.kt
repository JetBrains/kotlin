/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.uast.UArrayAccessExpression
import org.jetbrains.uast.UElement

abstract class KotlinAbstractUArrayAccessExpression(
    override val sourcePsi: KtArrayAccessExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UArrayAccessExpression, KotlinUElementWithType, KotlinEvaluatableUElement {
    override val receiver by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.arrayExpression, this)
    }

    override val indices by lz {
        sourcePsi.indexExpressions.map {
            baseResolveProviderService.baseKotlinConverter.convertOrEmpty(it, this)
        }
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UIfExpression

class KotlinUIfExpression(
    override val sourcePsi: KtIfExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UIfExpression, KotlinUElementWithType, KotlinEvaluatableUElement {
    override val condition by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.condition, this)
    }
    override val thenExpression by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrNull(sourcePsi.then, this)
    }
    override val elseExpression by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrNull(sourcePsi.`else`, this)
    }
    override val isTernary = false

    override val ifIdentifier: UIdentifier
        get() = UIdentifier(null, this)

    override val elseIdentifier: UIdentifier?
        get() = null
}

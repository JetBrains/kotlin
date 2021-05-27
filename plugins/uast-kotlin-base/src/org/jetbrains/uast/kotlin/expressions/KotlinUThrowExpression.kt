/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UThrowExpression

class KotlinUThrowExpression(
    override val sourcePsi: KtThrowExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UThrowExpression, KotlinUElementWithType {
    override val thrownExpression by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.thrownExpression, this)
    }
}

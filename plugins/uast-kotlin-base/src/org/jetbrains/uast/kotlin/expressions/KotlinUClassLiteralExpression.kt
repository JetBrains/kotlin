/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.uast.DEFAULT_EXPRESSION_TYPES_LIST
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression

class KotlinUClassLiteralExpression(
    override val sourcePsi: KtClassLiteralExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UClassLiteralExpression, KotlinUElementWithType {
    override val type by lz {
        baseResolveProviderService.getDoubleColonReceiverType(sourcePsi, this)
    }
    
    override val expression: UExpression?
        get() {
            if (type != null) return null
            val receiverExpression = sourcePsi.receiverExpression ?: return null
            return baseResolveProviderService.baseKotlinConverter.convertExpression(receiverExpression, this, DEFAULT_EXPRESSION_TYPES_LIST)
        }
}

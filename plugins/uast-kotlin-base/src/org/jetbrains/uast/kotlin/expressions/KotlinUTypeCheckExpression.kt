/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastBinaryExpressionWithTypeKind
import org.jetbrains.uast.UastErrorType

class KotlinUTypeCheckExpression(
    override val sourcePsi: KtIsExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UBinaryExpressionWithType, KotlinUElementWithType, KotlinEvaluatableUElement {
    override val operand by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.leftHandSide, this)
    }

    override val type by lz {
        sourcePsi.typeReference?.let {
            baseResolveProviderService.resolveToType(it, this)
        } ?: UastErrorType
    }

    override val typeReference by lz {
        sourcePsi.typeReference?.let {
            KotlinUTypeReferenceExpression(it, this) { type }
        }
    }

    override val operationKind =
        if (sourcePsi.isNegated)
            KotlinBinaryExpressionWithTypeKinds.NEGATED_INSTANCE_CHECK
        else
            UastBinaryExpressionWithTypeKind.INSTANCE_CHECK
}

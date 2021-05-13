/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiType
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.UastErrorType

class FirKotlinUTypeReferenceExpression(
    override val sourcePsi: KtTypeReference,
    givenParent: UElement?,
    private val typeSupplier: (() -> PsiType)? = null,
) : KotlinAbstractUExpression(givenParent), UTypeReferenceExpression {
    override val type: PsiType by lz {
        typeSupplier?.invoke() ?: sourcePsi.toPsiType(uastParent ?: this)
    }
}

private fun KtTypeReference?.toPsiType(
    source: UElement,
): PsiType {
    if (this == null) return UastErrorType
    // TODO: use type conversions in firLightUtils.kt
    return PsiType.NULL
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.kinds.KotlinSpecialExpressionKinds

class KotlinSupertypeDelegationUExpression(
    override val sourcePsi: KtDelegatedSuperTypeEntry,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UExpressionList {

    override val psi: PsiElement
        get() = sourcePsi

    val typeReference: UTypeReferenceExpression? by lz {
        sourcePsi.typeReference?.let {
            KotlinUTypeReferenceExpression(it, this) {
                baseResolveProviderService.resolveToType(it, this) ?: UastErrorType
            }
        }
    }

    val delegateExpression: UExpression? by lz {
        sourcePsi.delegateExpression?.let { languagePlugin?.convertElement(it, this, UExpression::class.java) as? UExpression }
    }

    override val expressions: List<UExpression>
        get() = listOfNotNull(typeReference, delegateExpression)

    override val kind: UastSpecialExpressionKind
        get() = KotlinSpecialExpressionKinds.SUPER_DELEGATION
}

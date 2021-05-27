/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.kotlin.internal.KotlinFakeUElement

class KotlinUImplicitReturnExpression(
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UReturnExpression, KotlinUElementWithType, KotlinFakeUElement {
    override val psi: PsiElement?
        get() = null

    override lateinit var returnExpression: UExpression

    // Due to the lack of [psi], (lazily) delegate to the one in [returnExpression]
    override val baseResolveProviderService: BaseKotlinUastResolveProviderService by lz {
        (returnExpression as KotlinAbstractUElement).baseResolveProviderService
    }

    override fun unwrapToSourcePsi(): List<PsiElement> {
        return returnExpression.toSourcePsiFakeAware()
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UThisExpression
import org.jetbrains.uast.kotlin.internal.DelegatedMultiResolve

class KotlinUThisExpression(
    override val sourcePsi: KtThisExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UThisExpression, DelegatedMultiResolve, KotlinUElementWithType, KotlinEvaluatableUElement {
    override val label: String?
        get() = sourcePsi.getLabelName()

    override val labelIdentifier: UIdentifier?
        get() = sourcePsi.getTargetLabel()?.let { KotlinUIdentifier(it, this) }

    override fun resolve() =
        baseResolveProviderService.resolveToDeclaration(sourcePsi)
}

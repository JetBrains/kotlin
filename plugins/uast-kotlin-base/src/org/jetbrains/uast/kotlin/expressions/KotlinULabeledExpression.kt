/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.ULabeledExpression

class KotlinULabeledExpression(
    override val sourcePsi: KtLabeledExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), ULabeledExpression {
    override val label: String
        get() = sourcePsi.getLabelName().orAnonymous("label")

    override val labelIdentifier: UIdentifier?
        get() = sourcePsi.getTargetLabel()?.let { KotlinUIdentifier(it, this) }

    override val expression by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.baseExpression, this)
    }
}

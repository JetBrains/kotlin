/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.uast.UDoWhileExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier

class KotlinUDoWhileExpression(
    override val sourcePsi: KtDoWhileExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UDoWhileExpression {
    override val condition by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.condition, this)
    }
    override val body by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.body, this)
    }

    override val doIdentifier: UIdentifier
        get() = KotlinUIdentifier(null, this)

    override val whileIdentifier: UIdentifier
        get() = KotlinUIdentifier(null, this)
}

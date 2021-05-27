/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UWhileExpression

class KotlinUWhileExpression(
    override val sourcePsi: KtWhileExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UWhileExpression {
    override val condition by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.condition, this)
    }
    override val body by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.body, this)
    }

    override val whileIdentifier: UIdentifier
        get() = KotlinUIdentifier(null, this)
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.uast.UElement

class FirKotlinUSimpleReferenceExpression(
    override val sourcePsi: KtSimpleNameExpression,
    givenParent: UElement?
) : KotlinAbstractUSimpleReferenceExpression(sourcePsi, givenParent) {
    // TODO: handle destructuring declaration differently or commonize it
}

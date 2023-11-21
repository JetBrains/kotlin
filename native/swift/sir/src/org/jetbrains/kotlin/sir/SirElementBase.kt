/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.visitors.SirVisitor
import org.jetbrains.kotlin.sir.visitors.SirTransformer

sealed class SirElementBase : SirElement {
    abstract override fun <R, D> accept(visitor: SirVisitor<R, D>, data: D): R

    abstract override fun <E : SirElement, D> transform(transformer: SirTransformer<D>, data: D): E
}

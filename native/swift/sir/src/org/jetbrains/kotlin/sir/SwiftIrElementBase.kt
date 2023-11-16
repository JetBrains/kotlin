/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.visitors.SwiftIrTransformer
import org.jetbrains.kotlin.sir.visitors.SwiftIrVisitor

sealed class SwiftIrElementBase : SwiftIrElement {
    abstract override fun <R, D> accept(visitor: SwiftIrVisitor<R, D>, data: D): R

    abstract override fun <E : SwiftIrElement, D> transform(transformer: SwiftIrTransformer<D>, data: D): E
}

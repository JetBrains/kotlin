/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.SwiftIrElement
import org.jetbrains.kotlin.sir.SwiftIrElementBase
import org.jetbrains.kotlin.sir.visitors.SwiftIrTransformer

// FIXME: Copy-pasted from FIR, use auto-generation instead.
fun <T : SwiftIrElement, D> MutableList<T>.transformInPlace(transformer: SwiftIrTransformer<D>, data: D) {
    val iterator = this.listIterator()
    while (iterator.hasNext()) {
        val next = iterator.next() as SwiftIrElementBase
        val result = next.transform<T, D>(transformer, data)
        if (result !== next) {
            iterator.set(result)
        }
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.SirElement
import org.jetbrains.kotlin.sir.SirElementBase
import org.jetbrains.kotlin.sir.visitors.SirTransformer

// FIXME: Copy-pasted from FIR, use auto-generation instead.
fun <T : SirElement, D> MutableList<T>.transformInPlace(transformer: SirTransformer<D>, data: D) {
    val iterator = this.listIterator()
    while (iterator.hasNext()) {
        val next = iterator.next() as SirElementBase
        val result = next.transform<T, D>(transformer, data)
        if (result !== next) {
            iterator.set(result)
        }
    }
}

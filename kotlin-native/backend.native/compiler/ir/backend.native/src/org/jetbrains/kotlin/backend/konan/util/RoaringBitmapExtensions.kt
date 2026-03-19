/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.util

import org.roaringbitmap.RoaringBitmap

fun RoaringBitmap.orWithFilterHasChanged(another: RoaringBitmap): Boolean {
    val sizeBefore = this.cardinality
    this.or(another)
    return this.cardinality != sizeBefore
}

fun RoaringBitmap.orWithFilterHasChanged(another: RoaringBitmap, filter: RoaringBitmap): Boolean {
    val sizeBefore = this.cardinality
    val toAdd = RoaringBitmap.and(another, filter)
    this.or(toAdd)
    return this.cardinality != sizeBefore
}

inline fun RoaringBitmap.forEachBit(block: (Int) -> Unit) {
    val it = this.intIterator
    while (it.hasNext()) {
        block(it.next())
    }
}

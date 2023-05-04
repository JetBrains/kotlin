/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kt51925.*
import kotlinx.cinterop.*

fun bar1(e: E) = e.value

inline fun foo1() = bar1(E.Z)

fun bar2(s: Struct): Int {
    return s.d
}

inline fun foo2(): Int {
    memScoped {
        val s = alloc<Struct>()
        s.d = 42
        return bar2(s)
    }
}

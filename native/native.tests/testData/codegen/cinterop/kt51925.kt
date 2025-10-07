/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: kt51925.def
strictEnums = E
---
enum E {
    X = 1, Y = 2, Z = 42
};

typedef struct {
    int d;
} Struct;

// MODULE: lib(cinterop)
// FILE: lib.kt
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

// MODULE: main(lib)
// FILE: main.kt
import kotlin.test.*

fun box(): String {
    assertEquals(42u, foo1())
    assertEquals(42, foo2())
    return "OK"
}
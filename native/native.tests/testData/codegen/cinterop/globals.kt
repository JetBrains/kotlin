/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: cglobals.def
---
const int g1 = 42;

int g2 = 17;

struct S {
    int x;
} g3 = { 128 };

int g4[2] = { 13, 14 };

int g5[2][2] = { 15, 16, 17, 18 };

struct S* const g6 = &g3;

void globals_foo() {
    // Test that local vars are not treated as global ones.
    float g1;
}

// Test non-compilable variable:
typedef int MyInt;
MyInt g7;
#define g7 bad macro

// Test property name mangling:
struct g1 {};
struct g1_ {};

typedef void* voidptr;
_Pragma("clang assume_nonnull begin")
const voidptr g8 = 0x1, g9 = 0x2;
_Pragma("clang assume_nonnull end")

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

import kotlinx.cinterop.*
import cglobals.*

fun box(): String {
    assert(g1__ == 42)

    assert(g2 == 17)
    g2 = 42
    assert(g2 == 42)

    assert(g3.x == 128)
    g3.x = 7
    assert(g3.x == 7)

    assert(g4[1] == 14)
    g4[1] = 15
    assert(g4[1] == 15)

    assert(g5[0] == 15)
    assert(g5[3] == 18)
    g5[0] = 16
    assert(g5[0] == 16)

    assert(g6 == g3.ptr)

    assert(g8.toLong() == 0x1L)
    assert(g9.toLong() == 0x2L)

    return "OK"
}

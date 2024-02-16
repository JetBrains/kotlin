/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: cstdlib.def
headers = stdlib.h

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import cstdlib.*
import kotlinx.cinterop.*
import kotlin.test.*

fun box(): String {
    assertEquals(257, atoi("257"))

    val divResult = div(-5, 3)
    val (quot, rem) = divResult.useContents { Pair(quot, rem) }
    assertEquals(-1, quot)
    assertEquals(-2, rem)

    return "OK"
}

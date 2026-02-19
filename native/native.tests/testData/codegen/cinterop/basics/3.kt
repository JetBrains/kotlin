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
    val values = intArrayOf(14, 12, 9, 13, 8)
    val count = values.size

    qsort(values.refTo(0), count.convert(), IntVar.size.convert(), staticCFunction { a, b ->
        val aValue = a!!.reinterpret<IntVar>()[0]
        val bValue = b!!.reinterpret<IntVar>()[0]

        (aValue - bValue)
    })

    assertEquals(8, values[0])
    assertEquals(9, values[1])
    assertEquals(12, values[2])
    assertEquals(13, values[3])
    assertEquals(14, values[4])

    return "OK"
}

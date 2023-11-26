// OUTPUT_DATA_FILE: vtable_any.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.test.*

fun box(): String {
    // Try to trick devirtualizer
    println(anyMethods(arrayListOf("1")))
    println(anyMethods(mapOf("2" to 1).keys))
    println(anyMethods(mapOf("1" to 3).values))
    println(anyMethods(setOf("4")))

    return "OK"
}

fun anyMethods(iterable: Iterable<*>): String {
    assert(iterable.equals(iterable))
    assert(iterable.hashCode() != 0)
    return iterable.toString()
}

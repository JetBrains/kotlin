/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, FreezingIsDeprecated::class)

import kotlin.test.*

import kotlin.native.concurrent.*

fun main() {
    val exceptionHook = { _: Throwable ->
        println("Hook")
    }.freeze()

    val oldHook = setUnhandledExceptionHook(exceptionHook)
    assertNull(oldHook)
    val hook1 = getUnhandledExceptionHook()
    assertEquals(exceptionHook, hook1)
    val hook2 = getUnhandledExceptionHook()
    assertEquals(exceptionHook, hook2)
    val hook3 = setUnhandledExceptionHook(null)
    assertEquals(exceptionHook, hook3)
    val hook4 = getUnhandledExceptionHook()
    assertNull(hook4)
}

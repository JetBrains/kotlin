/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, FreezingIsDeprecated::class)

import kotlin.test.*
import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*

fun main() {
    val called = AtomicInt(0)
    setUnhandledExceptionHook({ _: Throwable ->
        called.value = 1
    }.freeze())

    val exception = Error("some error")
    processUnhandledException(exception)
    assertEquals(1, called.value)
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(FreezingIsDeprecated::class, ExperimentalStdlibApi::class)

import kotlin.test.*

import kotlin.native.concurrent.*

fun customExceptionHook(throwable: Throwable) {
    println("Hook called")
    throw Error("another error")
}

fun main() {
    setUnhandledExceptionHook((::customExceptionHook).freeze())

    throw Error("some error")
}

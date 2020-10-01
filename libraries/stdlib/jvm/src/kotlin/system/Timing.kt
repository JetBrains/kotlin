/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmName("TimingKt")
package kotlin.system

import kotlin.contracts.*

/**
 * Executes the given [block] and returns elapsed time in milliseconds.
 *
 * @sample samples.system.Timing.measureBlockTimeMillis
 */
public inline fun measureTimeMillis(block: () -> Unit): Long {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}

/**
 * Executes the given [block] and returns elapsed time in nanoseconds.
 *
 * @sample samples.system.Timing.measureBlockNanoTime
 */
public inline fun measureNanoTime(block: () -> Unit): Long {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val start = System.nanoTime()
    block()
    return System.nanoTime() - start
}

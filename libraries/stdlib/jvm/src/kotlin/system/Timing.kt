/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmName("TimingKt")
package kotlin.system

import kotlin.contracts.*
import kotlin.time.*

/**
 * Executes the given [block] and returns elapsed time in milliseconds.
 *
 * This function is obsolete, and it is recommended to use [measureTime] instead as
 * it does not suffer from [measureTimeMillis] downsides and provides human-readable output.
 *
 * [measureTimeMillis] uses `System.currentTimeMillis` which is not monotonic, is a subject
 * to a clock drift, and has an OS-dependent coarse-grained resolution.
 * [measureTimeMillis] can return a negative or zero value as a result.
 *
 * @see measureTime
 * @sample samples.system.Timing.measureBlockTimeMillis
 */
public inline fun measureTimeMillis(block: () -> Any): Long {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}

/**
 * Executes the given [block] and returns elapsed time in nanoseconds.
 * For a more human-readable and typed output, [measureTime] can be used instead.
 *
 * @see measureTime
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

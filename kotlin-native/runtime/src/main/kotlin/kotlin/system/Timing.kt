/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.system

/**
 * Gets current system time in milliseconds since certain moment in the past,
 * only delta between two subsequent calls makes sense.
 */
@SymbolName("Kotlin_system_getTimeMillis")
public external fun getTimeMillis() : Long

/**
 * Gets current system time in nanoseconds since certain moment in the past,
 * only delta between two subsequent calls makes sense.
 */
@SymbolName("Kotlin_system_getTimeNanos")
public external fun getTimeNanos() : Long

/**
 * Gets current system time in microseconds since certain moment in the past,
 * only delta between two subsequent calls makes sense.
 */
@SymbolName("Kotlin_system_getTimeMicros")
public external fun getTimeMicros() : Long

/**
 * Executes the given [block] and returns elapsed time in milliseconds.
 *
 * @sample samples.system.Timing.measureBlockTimeMillis
 */
public inline fun measureTimeMillis(block: () -> Unit) : Long {
    val start = getTimeMillis()
    block()
    return getTimeMillis() - start
}

/** Executes the given [block] and returns elapsed time in microseconds (Kotlin/Native only). */
public inline fun measureTimeMicros(block: () -> Unit) : Long {
    val start = getTimeMicros()
    block()
    return getTimeMicros() - start
}

/**
 * Executes the given [block] and returns elapsed time in nanoseconds.
 *
 * @sample samples.system.Timing.measureBlockNanoTime
 */
public inline fun measureNanoTime(block: () -> Unit) : Long {
    val start = getTimeNanos()
    block()
    return getTimeNanos() - start
}

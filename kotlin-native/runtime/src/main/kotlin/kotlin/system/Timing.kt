/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.system

import kotlin.native.internal.GCUnsafeCall
import kotlin.time.*

/**
 * Gets current system time in milliseconds since certain moment in the past,
 * only delta between two subsequent calls makes sense.
 *
 * This function is deprecated.
 * To measure the duration of execution of a block of code,
 * use [measureTime] or [measureTimedValue] instead.
 * To mark a point in time for querying the duration of time interval [elapsed][TimeMark.elapsedNow]
 * from that point, use [TimeSource.Monotonic.markNow] instead.
 * The resulting [Duration] then can be expressed as a [Long] number of milliseconds
 * using [Duration.inWholeMilliseconds].
 */
@Deprecated("Use measureTime() or TimeSource.Monotonic.markNow() instead.")
@DeprecatedSinceKotlin(warningSince = "1.9")
@GCUnsafeCall("Kotlin_system_getTimeMillis")
public external fun getTimeMillis() : Long

/**
 * Gets current system time in nanoseconds since certain moment in the past,
 * only delta between two subsequent calls makes sense.
 *
 * This function is deprecated.
 * To measure the duration of execution of a block of code,
 * use [measureTime] or [measureTimedValue] instead.
 * To mark a point in time for querying the duration of time interval [elapsed][TimeMark.elapsedNow]
 * from that point, use [TimeSource.Monotonic.markNow] instead.
 * The resulting [Duration] then can be expressed as a [Long] number of nanoseconds
 * using [Duration.inWholeNanoseconds].
 */
@Deprecated("Use measureTime() or TimeSource.Monotonic.markNow() instead.")
@DeprecatedSinceKotlin(warningSince = "1.9")
@GCUnsafeCall("Kotlin_system_getTimeNanos")
public external fun getTimeNanos() : Long

/**
 * Gets current system time in microseconds since certain moment in the past,
 * only delta between two subsequent calls makes sense.
 *
 * This function is deprecated.
 * To measure the duration of execution of a block of code,
 * use [measureTime] or [measureTimedValue] instead.
 * To mark a point in time for querying the duration of time interval [elapsed][TimeMark.elapsedNow]
 * from that point, use [TimeSource.Monotonic.markNow] instead.
 * The resulting [Duration] then can be expressed as a [Long] number of microseconds
 * using [Duration.inWholeMicroseconds].
 */
@Deprecated("Use measureTime() or TimeSource.Monotonic.markNow() instead.")
@DeprecatedSinceKotlin(warningSince = "1.9")
@GCUnsafeCall("Kotlin_system_getTimeMicros")
public external fun getTimeMicros() : Long

/**
 * Executes the given [block] and returns elapsed time in milliseconds.
 *
 * This function is deprecated.
 * To measure the duration of execution of a block of code,
 * use [measureTime] or [measureTimedValue] instead.
 * The resulting [Duration] then can be expressed as a [Long] number of milliseconds
 * using [Duration.inWholeMilliseconds].
 *
 * @sample samples.system.Timing.measureBlockTimeMillis
 */
@Deprecated("Use measureTime() instead.", ReplaceWith("measureTime(block).inWholeMilliseconds"))
@DeprecatedSinceKotlin(warningSince = "1.9")
@Suppress("DEPRECATION")
public inline fun measureTimeMillis(block: () -> Unit) : Long {
    val start = getTimeMillis()
    block()
    return getTimeMillis() - start
}

/**
 * Executes the given [block] and returns elapsed time in microseconds (Kotlin/Native only).
 *
 * This function is deprecated.
 * To measure the duration of execution of a block of code,
 * use [measureTime] or [measureTimedValue] instead.
 * The resulting [Duration] then can be expressed as a [Long] number of microseconds
 * using [Duration.inWholeMicroseconds].
 */
@Deprecated("Use measureTime() instead.", ReplaceWith("measureTime(block).inWholeMicroseconds", "kotlin.time.measureTime"))
@DeprecatedSinceKotlin(warningSince = "1.9")
@Suppress("DEPRECATION")
public inline fun measureTimeMicros(block: () -> Unit) : Long {
    val start = getTimeMicros()
    block()
    return getTimeMicros() - start
}

/**
 * Executes the given [block] and returns elapsed time in nanoseconds.
 *
 * This function is deprecated.
 * To measure the duration of execution of a block of code,
 * use [measureTime] or [measureTimedValue] instead.
 * The resulting [Duration] then can be expressed as a [Long] number of nanoseconds
 * using [Duration.inWholeNanoseconds].
 *
 * @sample samples.system.Timing.measureBlockNanoTime
 */
@Deprecated("Use measureTime() instead.", ReplaceWith("measureTime(block).inWholeNanoseconds", "kotlin.time.measureTime"))
@DeprecatedSinceKotlin(warningSince = "1.9")
@Suppress("DEPRECATION")
public inline fun measureNanoTime(block: () -> Unit) : Long {
    val start = getTimeNanos()
    block()
    return getTimeNanos() - start
}

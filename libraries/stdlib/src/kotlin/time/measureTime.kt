/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.contracts.*

/**
 * Executes the given function [block] and returns the duration of elapsed time interval.
 *
 * The elapsed time is measured with [TimeSource.Monotonic].
 */
@SinceKotlin("1.3")
@ExperimentalTime
public inline fun measureTime(block: () -> Unit): Duration {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return TimeSource.Monotonic.measureTime(block)
}


/**
 * Executes the given function [block] and returns the duration of elapsed time interval.
 *
 * The elapsed time is measured with the specified `this` [TimeSource] instance.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public inline fun TimeSource.measureTime(block: () -> Unit): Duration {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val mark = markNow()
    block()
    return mark.elapsedNow()
}


/**
 * Data class representing a result of executing an action, along with the duration of elapsed time interval.
 *
 * @property value the result of the action.
 * @property duration the time elapsed to execute the action.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public data class TimedValue<T>(val value: T, val duration: Duration)

/**
 * Executes the given function [block] and returns an instance of [TimedValue] class, containing both
 * the result of the function execution and the duration of elapsed time interval.
 *
 * The elapsed time is measured with [TimeSource.Monotonic].
 */
@SinceKotlin("1.3")
@ExperimentalTime
public inline fun <T> measureTimedValue(block: () -> T): TimedValue<T> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return TimeSource.Monotonic.measureTimedValue(block)
}

/**
 * Executes the given [block] and returns an instance of [TimedValue] class, containing both
 * the result of function execution and the duration of elapsed time interval.
 *
 * The elapsed time is measured with the specified `this` [TimeSource] instance.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public inline fun <T> TimeSource.measureTimedValue(block: () -> T): TimedValue<T> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val mark = markNow()
    val result = block()
    return TimedValue(result, mark.elapsedNow())
}

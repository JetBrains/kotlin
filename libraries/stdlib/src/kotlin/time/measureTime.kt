/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.contracts.*

/**
 * Executes the given function [block] and returns the duration of elapsed time interval.
 *
 * The elapsed time is measured with [MonoClock].
 */
@SinceKotlin("1.3")
@ExperimentalTime
public inline fun measureTime(block: () -> Unit): Duration {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return MonoClock.measureTime(block)
}


/**
 * Executes the given function [block] and returns the duration of elapsed time interval.
 *
 * The elapsed time is measured with the specified `this` [Clock] instance.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public inline fun Clock.measureTime(block: () -> Unit): Duration {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val mark = mark()
    block()
    return mark.elapsed()
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
 * The elapsed time is measured with [MonoClock].
 */
@SinceKotlin("1.3")
@ExperimentalTime
public inline fun <T> measureTimedValue(block: () -> T): TimedValue<T> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return MonoClock.measureTimedValue(block)
}

/**
 * Executes the given [block] and returns an instance of [TimedValue] class, containing both
 * the result of function execution and the duration of elapsed time interval.
 *
 * The elapsed time is measured with the specified `this` [Clock] instance.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public inline fun <T> Clock.measureTimedValue(block: () -> T): TimedValue<T> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val mark = mark()
    val result = block()
    return TimedValue(result, mark.elapsed())
}

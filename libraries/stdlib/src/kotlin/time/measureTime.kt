/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.contracts.*

public inline fun measureTime(action: () -> Unit): Duration {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return MonoClock.measureTime(action)
}


public inline fun Clock.measureTime(action: () -> Unit): Duration {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }

    val mark = mark()
    action()
    return mark.elapsed()
}



public data class DurationMeasured<T>(val value: T, val duration: Duration)

public inline fun <T> withMeasureTime(action: () -> T): DurationMeasured<T> {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }

    return MonoClock.withMeasureTime(action)
}

public inline fun <T> Clock.withMeasureTime(action: () -> T): DurationMeasured<T> {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }

    val mark = mark()
    val result = action()
    return DurationMeasured(result, mark.elapsed())
}

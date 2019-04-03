/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

public interface Clock {

    /**
     * Marks a time point on this clock.
     *
     * @param initialElapsed An initial value of [ClockMark.elapsedFrom] property of the resulting [ClockMark]:
     *
     * - pass [Duration.ZERO] to mark a time point that is now.
     * - pass a positive duration to mark a time point in the past.
     * - pass a negative duration to mark a time point in the future.
     *
     */
    fun mark(initialElapsed: Duration = Duration.ZERO): ClockMark

    companion object {
        val Default: Clock get() = MonoClock
    }

}

public interface ClockMark {
    val clock: Clock
    val elapsedFrom: Duration
}
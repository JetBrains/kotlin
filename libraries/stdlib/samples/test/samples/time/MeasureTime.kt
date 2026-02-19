/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.time

import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.*

// Note: samples are timing-heavy and do not assert anything, thus they are not run as test, only compiled
@Suppress("unused")
class MeasureTime {

    fun measureTimeSample() {
        fun slowFunction(): Unit = Thread.sleep(1000L)
        val elapsed = measureTime {
            slowFunction()
        }
        println("Time elapsed: ${elapsed.inWholeMilliseconds} milliseconds ($elapsed)")
    }

    fun measureTimedValueSample() {
        fun slowFunction(): Unit = Thread.sleep(1000L)
        val result = measureTimedValue {
            slowFunction()
            42
        }
        println("Computed result: ${result.value}, time elapsed: ${result.duration}")
    }

    fun explicitMeasureTimeSample() {
        val testSource = TestTimeSource()
        val elapsed = testSource.measureTime {
            println("Pretending this function executes 10 seconds")
            testSource += 10.seconds
        }
        println("Time elapsed: ${elapsed.inWholeMilliseconds} milliseconds ($elapsed)")
    }

    fun explicitMeasureTimedValueSample() {
        val testSource = TestTimeSource()
        val result = testSource.measureTimedValue {
            println("Pretending this function executes 10 seconds")
            testSource += 10.seconds
            42
        }
        println("Computed result: ${result.value}, time elapsed: ${result.duration}")
    }

    fun monotonicMeasureTimeSample() {
        fun slowFunction(): Unit = Thread.sleep(1000L)
        val elapsed = TimeSource.Monotonic.measureTime {
            slowFunction()
        }
        println("Time elapsed: ${elapsed.inWholeMilliseconds} milliseconds ($elapsed)")
    }

    fun monotonicMeasureTimedValueSample() {
        fun slowFunction(): Unit = Thread.sleep(1000L)
        val result = TimeSource.Monotonic.measureTimedValue() {
            slowFunction()
            42
        }
        println("Computed result: ${result.value}, time elapsed: ${result.duration}")
    }
}
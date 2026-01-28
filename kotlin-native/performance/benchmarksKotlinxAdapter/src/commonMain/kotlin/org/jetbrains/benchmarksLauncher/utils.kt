package org.jetbrains.benchmarksLauncher

import kotlin.native.concurrent.ThreadLocal
import kotlinx.benchmark.Param

class Blackhole {
    @ThreadLocal
    companion object {
        var consumer = 0
        fun consume(value: Any) {
            consumer += value.hashCode()
        }
    }
}

class Random constructor() {
    @ThreadLocal
    companion object {
        var seedInt = 0
        fun nextInt(boundary: Int = 100): Int {
            seedInt = (3 * seedInt + 11) % boundary
            return seedInt
        }

        var seedDouble: Double = 0.1
        fun nextDouble(boundary: Double = 100.0): Double {
            seedDouble = (7.0 * seedDouble + 7.0) % boundary
            return seedDouble
        }
    }
}

abstract class SkipWhenBaseOnly {
    @Param("false")
    var baseOnly = false

    fun skipWhenBaseOnly() {
        check(!baseOnly) { "Skipping because baseOnly=true" }
    }
}
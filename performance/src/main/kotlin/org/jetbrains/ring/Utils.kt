package org.jetbrains.ring

import kotlin.system.measureNanoTime

//-----------------------------------------------------------------------------//

class Blackhole {
    companion object {
        var consumer = 0
        fun consume(value: Any) {
            consumer += value.hashCode()
        }
    }
}

//-----------------------------------------------------------------------------//

class Random() {
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

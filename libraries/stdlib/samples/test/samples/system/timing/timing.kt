package samples.system.timing

import samples.*
import kotlin.system.*
import kotlin.test.*

class Timings {

    @Sample
    fun measureBlockTimeMillis() {
        val timeInMillis = measureTimeMillis {
            (0..10).reduce(Int::plus)
        }
        assertTrue(timeInMillis >= 0)
        assertTrue(timeInMillis < 1e9)
    }

    @Sample
    fun measureBlockNanoTime() {
        val timeInNanos = measureNanoTime {
            (0..5).map { it * 2 }
        }
        assertTrue(timeInNanos >= 0)
        assertTrue(timeInNanos < 1e12)
    }
}
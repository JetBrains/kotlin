package samples.system.timing

import samples.*
import kotlin.system.*

class Timings {

    @Sample
    fun measureBlockTimeMillis() {
        val timeInMillis = measureTimeMillis {
            val numbers = (0..100).toMutableList()
            numbers.shuffle()
            numbers.sort()
        }
        println("The operation took $timeInMillis ms")
    }

    @Sample
    fun measureBlockNanoTime() {
        val timeInNanos = measureNanoTime {
            (0..5).map { it * 2 }
        }
        println("The operation took $timeInNanos ns")
    }
}
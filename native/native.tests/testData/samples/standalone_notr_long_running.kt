// KIND: STANDALONE_NO_TR
// EXPECTED_TIMEOUT_FAILURE

import kotlin.math.E
import kotlin.math.sqrt
import kotlin.system.getTimeMillis
import kotlin.test.assertTrue

// Runs for ~240 seconds. Prints a short message to stdout every second.
// 24o seconds is expected to exceed the default test timeout and multipliers used in special modes.
fun main() {
    for (i in 1..240) {
        println("Iteration $i")
        sleep(1000)
    }
    println("Done.")
}

private fun sleep(millis: Int) {
    assertTrue(millis > 0)

    val endTimeMillis = getTimeMillis() + millis
    do {
        // Emulate intensive computations to spend CPU time.
        for (i in 1..100) {
            for (j in 1..100) {
                storage = if (storage.toLong() % 2 == 0L) sqrt(i.toDouble() * j.toDouble()) else E * i / j
            }
        }
    } while (getTimeMillis() < endTimeMillis)
}

private var storage: Double = 0.0

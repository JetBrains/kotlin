// KIND: STANDALONE_NO_TR
// EXPECTED_TIMEOUT_FAILURE: 5s

import kotlin.math.E
import kotlin.math.sqrt
import kotlin.test.assertTrue
import kotlin.time.*
import kotlin.time.Duration.Companion.milliseconds

// Runs for ~60 seconds. Prints a short message to stdout every second.
// It is expected to exceed the 5s timeout specified above.
fun main() {
    for (i in 1..60) {
        println("Iteration $i")
        sleep(1000)
    }
    println("Done.")
}

private fun sleep(millis: Int) {
    assertTrue(millis > 0)

    val timeMark = TimeSource.Monotonic.markNow() + millis.milliseconds
    do {
        // Emulate intensive computations to spend CPU time.
        for (i in 1..100) {
            for (j in 1..100) {
                storage = if (storage.toLong() % 2 == 0L) sqrt(i.toDouble() * j.toDouble()) else E * i / j
            }
        }
    } while (timeMark.hasNotPassedNow())
}

private var storage: Double = 0.0

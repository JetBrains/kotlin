package x.y.z.project

import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class FooTest {

    @Test
    fun delayedTest1() {
        delay(10.seconds)
        println("Test finished!")
    }

    @Test
    fun delayedTest2() {
        delay(10.seconds)
        println("Test finished!")
    }

    @Test
    fun delayedTest3() {
        delay(10.seconds)
        println("Test finished!")
    }

    companion object {
        // quick and basic KMP delay
        private fun delay(duration: Duration) {
            val timeMark = TimeSource.Monotonic.markNow()
            while (timeMark.elapsedNow() < duration) {
                // ...
            }
        }
    }
}

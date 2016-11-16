@file:kotlin.jvm.JvmVersion
package test.concurrent

import kotlin.concurrent.*
import kotlin.test.*

import java.util.concurrent.atomic.AtomicInteger
import java.util.Timer

import org.junit.Test

class TimerTest {
    @Test fun scheduledTask() {
        val counter = AtomicInteger(0)
        val timer = Timer()

        val task = timer.scheduleAtFixedRate(1000, 100) {
            val current = counter.incrementAndGet()
            // println("Timer fired at $current")
        }
        Thread.sleep(1500)
        task.cancel()

        val value = counter.get()
        assertTrue(value > 4, "Expected to fire at least 4 times, but was $value")
    }
}
package concurrent

import kotlin.concurrent.*
import kotlin.test.*

import java.util.concurrent.atomic.AtomicInteger
import java.util.Timer

import org.junit.Test as test

class TimerTest {
    test fun scheduledTask() {
        val counter = AtomicInteger(0)
        val timer = Timer()
        val task = timer.scheduleAtFixedRate(1000, 1000) {
            val current = counter.incrementAndGet()
            println("Tiemer fired at $current")
        }

        Thread.sleep(5000)
        task.cancel()

        val value = counter.get()
        assertTrue(value > 2, "currnet counter is $value")
    }
}
package test.concurrent

import kotlin.concurrent.*
import kotlin.test.*

import java.util.concurrent.atomic.AtomicInteger
import java.util.Timer

import org.junit.Test as test

class TimerTest {
    test fun scheduledTask() {
        val counter = AtomicInteger(0)
        val timer = Timer()
        /*
        TODO this generates a compiler error!

            val task = timer.scheduleAtFixedRate(1000, 1000) {
            val current = counter.incrementAndGet()
            println("Timer fired at $current")
        }
        */
        val task = timerTask {
            val current = counter.incrementAndGet()
            println("Timer fired at $current")
        }
        timer.scheduleAtFixedRate(task, 1000, 1000)
        Thread.sleep(5000)
        task.cancel()

        val value = counter.get()
        assertTrue(value > 2, "current counter is $value")
    }
}
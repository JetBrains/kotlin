package concurrent

import kotlin.concurrent.*
import kotlin.test.*

import org.junit.Test as test

import java.util.concurrent.*
import java.util.concurrent.TimeUnit.*

class ThreadTest {
    test fun scheduledTask() {

        val pool = Executors.newFixedThreadPool(1).sure()
        val countDown = CountDownLatch(1)
        pool {
            countDown.countDown()
        }
        assertTrue(countDown.await(2, SECONDS), "Count down is executed")
    }
}
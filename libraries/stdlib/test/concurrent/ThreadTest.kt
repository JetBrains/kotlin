@file:kotlin.jvm.JvmVersion
package test.concurrent

import kotlin.concurrent.*
import kotlin.test.*

import org.junit.Test

import java.util.concurrent.*
import java.util.concurrent.TimeUnit.*

class ThreadTest {
    @Test fun scheduledTask() {

        val pool = Executors.newFixedThreadPool(1)
        val countDown = CountDownLatch(1)
        pool.execute {
            countDown.countDown()
        }
        assertTrue(countDown.await(2, SECONDS), "Count down is executed")
    }

    @Test fun callableInvoke() {

        val pool = Executors.newFixedThreadPool(1)
        val future = pool.submit<String> {  // type specification required here to choose overload for callable, see KT-7882
           "Hello"
        }
        assertEquals("Hello", future.get(2, SECONDS))
    }

    @Test fun threadLocalGetOrSet() {
        val v = ThreadLocal<String>()

        assertEquals("v1", v.getOrSet { "v1" })
        assertEquals("v1", v.get())
        assertEquals("v1", v.getOrSet { "v2" })

        v.set(null)
        assertEquals("v2", v.getOrSet { "v2" })

        v.set("v3")
        assertEquals("v3", v.getOrSet { "v2" })


        val w = object: ThreadLocal<String>() {
            override fun initialValue() = "default"
        }

        assertEquals("default", w.getOrSet { "v1" })
    }
}
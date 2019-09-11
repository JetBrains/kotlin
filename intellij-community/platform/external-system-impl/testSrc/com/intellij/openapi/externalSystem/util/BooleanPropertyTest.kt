// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

import com.intellij.openapi.externalSystem.util.properties.BooleanProperty
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class BooleanPropertyTest {

  @Test
  fun `test parallel setup`() {
    repeat(1000) {
      val latch = CountDownLatch(1)
      val setEventCounter = AtomicInteger(0)
      val resetEventCounter = AtomicInteger(0)
      val observable = BooleanProperty(false)
      observable.afterSet { setEventCounter.incrementAndGet() }
      observable.afterReset { resetEventCounter.incrementAndGet() }

      run {
        val threads = generate(10) {
          thread {
            latch.await()
            observable.set()
          }
        }
        latch.countDown()
        threads.forEach(Thread::join)

        assertEquals(true, observable.get())
        assertEquals(1, setEventCounter.get())
        assertEquals(0, resetEventCounter.get())
      }
      run {
        val threads = generate(10) {
          thread {
            latch.await()
            observable.reset()
          }
        }
        latch.countDown()
        threads.forEach(Thread::join)

        assertEquals(false, observable.get())
        assertEquals(1, setEventCounter.get())
        assertEquals(1, resetEventCounter.get())
      }
    }
  }

  private fun <R> generate(times: Int, action: (Int) -> R): Iterable<R> {
    return (0 until times).map(action)
  }
}
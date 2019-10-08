// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class CompoundParallelOperationTraceTest : CompoundParallelOperationTraceTestCase() {

  @Test
  fun `test simple operation`() = testTrace<Int> {
    operation {
      trace.startTask(1)
      trace.finishTask(1)
    }
  }

  @Test
  fun `test partial operation`() = testTrace<Int> {
    trace.startTask(1)
    trace.finishTask(1)
    operation {
      trace.startTask(2)
      trace.finishTask(2)
    }
    trace.startTask(3)
    trace.finishTask(3)
  }

  @Test
  fun `test compound operation`() = testTrace<Int> {
    operation {
      trace.startTask(1)
      trace.startTask(2)
      trace.finishTask(2)
      trace.finishTask(1)
    }
  }

  @Test
  fun `test shuffled compound operation`() = testTrace<Int> {
    operation {
      trace.startTask(1)
      trace.startTask(2)
      trace.finishTask(1)
      trace.startTask(3)
      trace.finishTask(3)
      trace.finishTask(2)
    }
  }

  @Test
  fun `test lateness compound operation`() = testTrace<Int> {
    trace.startTask(1)
    operation {
      trace.finishTask(1)
      trace.startTask(2)
      trace.finishTask(2)
    }
  }

  @Test
  fun `test duplicated compound operation`() = testTrace<Int> {
    operation {
      trace.startTask(1)
      operation {
        trace.finishTask(1)
        trace.startTask(2)
        trace.finishTask(2)
      }
    }
  }

  @Test
  fun `test super compound operation`() = testTrace<Int> {
    operation {
      trace.startTask(1)
      operation {
        trace.startTask(2)
        operation {
          trace.startTask(3)
          trace.finishTask(3)
          trace.finishTask(1)
          trace.finishTask(2)
        }
      }
    }
  }

  @Test
  fun `test unidentified operation`() = testTrace<Nothing?> {
    operation {
      trace.startTask(null)
      operation {
        trace.startTask(null)
        trace.finishTask(null)
        trace.finishTask(null)
      }
    }
  }

  @Test
  fun `test parallel execution`() = testTrace<Int> {
    repeat(1000) {
      val latch = CountDownLatch(1)
      val startedOperations = AtomicInteger(0)
      val completedOperations = AtomicInteger(0)
      trace.beforeOperation {
        startedOperations.incrementAndGet()
      }
      trace.afterOperation {
        completedOperations.incrementAndGet()
      }

      operation {
        generate(10, trace::startTask)
        val threads = generate(10) {
          thread {
            latch.await()
            trace.finishTask(it)
          }
        }
        latch.countDown()
        threads.forEach(Thread::join)
      }

      assertEquals(1, startedOperations.get())
      assertEquals(1, completedOperations.get())
    }
  }

  @Test
  fun `test super compound parallel execution`() {
    val trace = CompoundParallelOperationTrace<Int>()
    repeat(1000) {
      val latch = CountDownLatch(1)
      val startedOperations = AtomicInteger(0)
      val completedOperations = AtomicInteger(0)
      trace.beforeOperation {
        startedOperations.incrementAndGet()
      }
      trace.afterOperation {
        completedOperations.incrementAndGet()
      }

      val threads = generate(10) {
        thread {
          latch.await()
          trace.startOperation()
          trace.startTask(it)
        }
      }
      latch.countDown()
      threads.forEach(Thread::join)
      generate(10, trace::finishTask)
      assertEquals(it.toString(), true, trace.isOperationCompleted())

      assertEquals(1, startedOperations.get())
      assertEquals(1, completedOperations.get())
    }
  }
}
/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.testFramework

import com.intellij.util.containers.ContainerUtil
import com.intellij.util.lang.CompoundRuntimeException
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.concurrency.Promise
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun assertConcurrent(vararg runnables: () -> Any?, maxTimeoutSeconds: Int = 5) {
  val numThreads = runnables.size
  val exceptions = ContainerUtil.createLockFreeCopyOnWriteList<Throwable>()
  val threadPool = Executors.newFixedThreadPool(numThreads)
  try {
    val allExecutorThreadsReady = CountDownLatch(numThreads)
    val afterInitBlocker = CountDownLatch(1)
    val allDone = CountDownLatch(numThreads)
    for (submittedTestRunnable in runnables) {
      threadPool.submit {
        allExecutorThreadsReady.countDown()
        try {
          afterInitBlocker.await()
          submittedTestRunnable()
        }
        catch (e: Throwable) {
          exceptions.add(e)
        }
        finally {
          allDone.countDown()
        }
      }
    }

    // wait until all threads are ready
    assertThat(allExecutorThreadsReady.await((runnables.size * 1000).toLong(), TimeUnit.MILLISECONDS)).isTrue()
    // start all test runners
    afterInitBlocker.countDown()
    assertThat(allDone.await(maxTimeoutSeconds.toLong(), TimeUnit.SECONDS)).isTrue()
  }
  finally {
    threadPool.shutdownNow()
  }
  CompoundRuntimeException.throwIfNotEmpty(exceptions)
}

fun assertConcurrentPromises(vararg runnables: () -> Promise<String>, maxTimeoutSeconds: Int = 5) {
  val numThreads = runnables.size
  val exceptions = ContainerUtil.createLockFreeCopyOnWriteList<Throwable>()
  val threadPool = Executors.newFixedThreadPool(numThreads)
  try {
    val allExecutorThreadsReady = CountDownLatch(numThreads)
    val afterInitBlocker = CountDownLatch(1)
    val allDone = CountDownLatch(numThreads)
    val promises: MutableList<Promise<String>> = Collections.synchronizedList(ArrayList<Promise<String>>())
    for (submittedTestRunnable in runnables) {
      threadPool.submit {
        allExecutorThreadsReady.countDown()
        try {
          afterInitBlocker.await()
          promises.add(submittedTestRunnable())
        }
        catch (e: Throwable) {
          exceptions.add(e)
        }
        finally {
          allDone.countDown()
        }
      }
    }

    // wait until all threads are ready
    assertThat(allExecutorThreadsReady.await(runnables.size.toLong(), TimeUnit.SECONDS)).isTrue()
    // start all test runners
    afterInitBlocker.countDown()
    assertThat(allDone.await(maxTimeoutSeconds.toLong(), TimeUnit.SECONDS)).isTrue()
    promises.forEach { promise -> promise.blockingGet(10, TimeUnit.SECONDS) }
  }
  finally {
    threadPool.shutdownNow()
  }
  CompoundRuntimeException.throwIfNotEmpty(exceptions)
}


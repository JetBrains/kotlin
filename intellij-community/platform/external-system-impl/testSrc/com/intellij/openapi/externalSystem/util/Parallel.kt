// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.concurrency.AsyncPromise
import java.util.concurrent.CountDownLatch

class Parallel private constructor() {
  private val promises = ArrayList<AsyncPromise<Any?>>()
  private val start = CountDownLatch(1)

  fun thread(block: () -> Unit) {
    val promise = AsyncPromise<Any?>()
    promises.add(promise)
    kotlin.concurrent.thread {
      start.await()
      try {
        block()
      }
      finally {
        promise.setResult(null)
      }
    }
  }

  companion object {
    /**
     * At the same time starts threads and waits for their completion
     */
    fun parallel(configure: Parallel.() -> Unit) {
      val pool = Parallel()
      pool.configure()
      pool.start.countDown()
      for (promise in pool.promises) {
        PlatformTestUtil.waitForPromise(promise)
      }
    }
  }
}
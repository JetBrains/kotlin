// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import groovy.util.GroovyTestCase.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class DisposeUtilsTest {
  @Test
  fun `test or dispose`() {
    val counter1 = AtomicInteger(0)
    val counter2 = AtomicInteger(0)
    val counter = AtomicInteger(0)
    val disposable1 = Disposable { counter1.incrementAndGet() }
    val disposable2 = Disposable { counter2.incrementAndGet() }
    val disposable = Disposable { counter.incrementAndGet() }
    val orDisposable = createOrDisposable(disposable1, disposable2)
    Disposer.register(orDisposable, disposable)
    Disposer.dispose(disposable1)
    assertEquals(1, counter1.get())
    assertEquals(0, counter2.get())
    assertEquals(1, counter.get())
    Disposer.dispose(disposable2)
    assertEquals(1, counter1.get())
    assertEquals(1, counter2.get())
    assertEquals(1, counter.get())
  }
}
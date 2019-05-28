// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

import org.junit.Assert.assertEquals

abstract class CompoundParallelOperationTraceTestCase {
  protected fun <R> repeat(times: Int, action: (Int) -> R): Iterable<R> {
    return (0 until times).map(action)
  }

  protected fun <Id> testTrace(action: TestTraceContext<Id>.() -> Unit) {
    TestTraceContext(true, CompoundParallelOperationTrace<Id>()).action()
  }

  protected class TestTraceContext<Id>(
    private val mustBeComplete: Boolean,
    private val delegate: CompoundParallelOperationTrace<Id>
  ) {

    val trace: CompoundParallelOperationTrace<Id>
      get() {
        assertEquals(mustBeComplete, delegate.isOperationCompleted())
        return delegate
      }

    fun operation(action: TestTraceContext<Id>.() -> Unit) {
      trace.startOperation()
      TestTraceContext(false, delegate).action()
      assertEquals(true, delegate.isOperationCompleted())
    }
  }
}
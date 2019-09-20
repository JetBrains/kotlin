// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

import com.intellij.util.LocalTimeCounter

class CompoundParallelOperationTrace<Id> {

  private val finishListeners = ArrayList<() -> Unit>()
  private val traces = LinkedHashMap<Id, Long>()
  private var startTime: Long? = null

  fun startOperation() {
    synchronized(this) {
      startTime = LocalTimeCounter.currentTime()
    }
  }

  fun isOperationCompleted(): Boolean {
    synchronized(this) {
      return startTime == null
    }
  }

  fun onOperationCompleted(listener: Listener) {
    onOperationCompleted(listener::listen)
  }

  fun onOperationCompleted(listener: () -> Unit) {
    synchronized(this) {
      finishListeners.add(listener)
    }
  }

  fun startTask(taskId: Id) {
    synchronized(this) {
      traces[taskId] = LocalTimeCounter.currentTime()
    }
  }

  fun finishTask(taskId: Id) {
    synchronized(this) {
      val taskStartTime = traces.remove(taskId) ?: return
      val operationStartTime = startTime ?: return
      if (taskStartTime < operationStartTime) return
      if (traces.isEmpty()) {
        startTime = null
        finishListeners.forEach { it() }
      }
    }
  }

  interface Listener {
    fun listen()
  }
}
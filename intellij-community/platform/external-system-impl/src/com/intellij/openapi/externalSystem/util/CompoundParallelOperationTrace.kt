// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

import com.intellij.util.LocalTimeCounter
import java.util.concurrent.CopyOnWriteArrayList

class CompoundParallelOperationTrace<Id : Any> {

  private val finishListeners = CopyOnWriteArrayList<Listener>()
  private val tracedUpdates = LinkedHashMap<Id, Long>()
  private var startTime: Long? = null

  fun startOperation() {
    synchronized(this) {
      if (startTime == null) {
        startTime = LocalTimeCounter.currentTime()
      }
    }
  }

  fun isCompleteOperation(): Boolean {
    return startTime == null
  }

  fun onCompleteOperation(listener: Listener) {
    finishListeners.add(listener)
  }

  fun startTask(taskId: Id) {
    synchronized(this) {
      tracedUpdates[taskId] = LocalTimeCounter.currentTime()
    }
  }

  fun finishTask(taskId: Id) {
    synchronized(this) {
      val taskStartTime = tracedUpdates.remove(taskId) ?: return
      val operationStartTime = startTime ?: return
      if (taskStartTime < operationStartTime) return
      if (tracedUpdates.isEmpty()) {
        startTime = null
      }
    }
    if (isCompleteOperation()) {
      finishListeners.forEach(Listener::onComplete)
    }
  }

  interface Listener {
    fun onComplete()
  }
}
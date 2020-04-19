// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class PerThreadTime {
  val threadIdToTime: Map<Long, TimeNano>
    get() = HashMap(_threadIdToTime)

  private val _threadIdToTime: ConcurrentMap<Long, TimeNano> = ConcurrentHashMap()

  fun addTimeSpentInCurrentThread(nanoTime: TimeNano) {
    val currentThread = Thread.currentThread()
    _threadIdToTime.compute(currentThread.id) { _, currentTime -> (currentTime ?: 0L) + nanoTime }
  }
}
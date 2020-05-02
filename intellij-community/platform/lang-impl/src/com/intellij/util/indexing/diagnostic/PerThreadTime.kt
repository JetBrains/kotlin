// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class PerThreadTime {
  private val timeBucketSize = 1024

  val threadIdToTimeBucket: Map<Long, MaxNTimeBucket>
    get() = HashMap(_threadIdToTimeBucket)

  private val _threadIdToTimeBucket: ConcurrentMap<Long, MaxNTimeBucket> = ConcurrentHashMap()

  fun addTimeSpentInCurrentThread(nanoTime: TimeNano) {
    val currentThread = Thread.currentThread()
    _threadIdToTimeBucket
      .computeIfAbsent(currentThread.id) { MaxNTimeBucket(timeBucketSize, nanoTime) }
      .addTime(nanoTime)
  }
}
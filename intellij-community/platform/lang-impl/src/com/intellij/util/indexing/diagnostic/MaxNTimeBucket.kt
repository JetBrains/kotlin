// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import java.util.*

/**
 * Bucket of times with size of at most [sizeLimit].
 * The first added time is `firstTime`.
 * Maintains exact min, max, mean of all added times.
 * Maintains median of [sizeLimit] max times.
 */
class MaxNTimeBucket(private val sizeLimit: Int, firstTime: TimeNano) {

  private val _maxNTimes: PriorityQueue<TimeNano> = PriorityQueue(setOf(firstTime))
  private var _count: Long = 1
  private var _minTime: TimeNano = firstTime
  private var _maxTime: TimeNano = firstTime
  private var _sum: TimeNano = firstTime

  @Synchronized
  fun addTime(time: TimeNano) {
    _maxNTimes += time

    if (_maxTime < time) _maxTime = time
    if (_minTime > time) _minTime = time
    _sum += time
    _count++

    while (_maxNTimes.size > sizeLimit) {
      _maxNTimes.poll()
    }
  }

  val count: Long @Synchronized get() = _count
  val totalTime: TimeNano @Synchronized get() = _sum
  val minTime: TimeNano @Synchronized get() = _minTime
  val maxTime: TimeNano @Synchronized get() = _maxTime
  val meanTime: Double @Synchronized get() = _sum.toDouble() / _count
  val maxNTimes: List<TimeNano> @Synchronized get() = _maxNTimes.toList()
}
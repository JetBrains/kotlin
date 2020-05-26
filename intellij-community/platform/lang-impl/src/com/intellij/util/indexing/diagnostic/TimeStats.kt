// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import java.util.*

/**
 * Bucket of times with size of at most [sizeLimit].
 * Maintains exact min, max, mean of all added times.
 */
class TimeStats(private val sizeLimit: Int) {

  private val _maxNTimes: PriorityQueue<TimeNano> = PriorityQueue()
  private var _count: Long? = null
  private var _minTime: TimeNano? = null
  private var _maxTime: TimeNano? = null
  private var _sum: TimeNano? = null

  @Synchronized
  fun addTime(time: TimeNano) {
    _maxNTimes += time

    if (_maxTime == null || _maxTime!! < time) _maxTime = time
    if (_minTime == null || _minTime!! > time) _minTime = time
    _sum = (_sum ?: 0) + time
    _count = (_count ?: 0) + 1

    while (_maxNTimes.size > sizeLimit) {
      _maxNTimes.poll()
    }
  }

  private fun failEmpty(): Nothing = throw IllegalStateException("No times have been added yet")

  val isEmpty: Boolean @Synchronized get() =  _maxNTimes.isEmpty()
  val minTime: TimeNano @Synchronized get() = _minTime ?: failEmpty()
  val maxTime: TimeNano @Synchronized get() = _maxTime ?: failEmpty()
  val meanTime: Double @Synchronized get() = (_sum ?: failEmpty()).toDouble() / (_count ?: failEmpty())
  val maxNTimes: List<TimeNano> @Synchronized get() = _maxNTimes.takeUnless { isEmpty }?.toList() ?: failEmpty()
}
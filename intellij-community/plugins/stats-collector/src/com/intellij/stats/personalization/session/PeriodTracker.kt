// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

import kotlin.math.max
import kotlin.math.min

class PeriodTracker {
  private val durations: MutableList<Long> = mutableListOf()

  fun minDuration(currentPeriod: Long?): Long? {
    val pastMin = durations.min()
    if (pastMin == null) return currentPeriod
    if (currentPeriod != null) {
      return min(pastMin, currentPeriod)
    }

    return pastMin
  }

  fun maxDuration(currentPeriod: Long?): Long? {
    val pastMax = durations.max()
    if (pastMax == null) return currentPeriod
    if (currentPeriod != null) {
      return max(pastMax, currentPeriod)
    }

    return pastMax
  }

  fun average(currentPeriod: Long?): Double {
    if (durations.isEmpty()) return currentPeriod?.toDouble() ?: 0.0
    val pastAvg = durations.average()
    if (currentPeriod == null) return pastAvg
    val n = durations.size
    return pastAvg * n / (n + 1) + currentPeriod / (n + 1)
  }

  fun count(currentPeriod: Long?): Int = durations.size + (if (currentPeriod != null) 1 else 0)

  fun totalTime(currentPeriod: Long?): Long = durations.sum() + (currentPeriod ?: 0)

  fun addDuration(duration: Long) {
    durations.add(duration)
  }
}
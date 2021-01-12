// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

class QueriesDuration(private var lastUpdateTimestamp: Long) : CompletionQueryTracker.Durations {
  override fun getCurrentQueryDuration(): Long = System.currentTimeMillis() - lastUpdateTimestamp
  override fun getAverageQueryDuration(): Double = tracker.average(currentPeriodDuration())
  override fun getMinQueryDuration(): Long = tracker.minDuration(currentPeriodDuration())!!
  override fun getMaxQueryDuration(): Long = tracker.maxDuration(currentPeriodDuration())!!

  private val tracker: PeriodTracker = PeriodTracker()

  fun fireQueryChanged() {
    val now = System.currentTimeMillis()
    tracker.addDuration(now - lastUpdateTimestamp)
    lastUpdateTimestamp = now
  }

  private fun currentPeriodDuration(): Long {
    return System.currentTimeMillis() - lastUpdateTimestamp
  }
}
// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

import com.intellij.openapi.diagnostic.logger

class CompletionSelectionTrackerImpl : CompletionSelectionTracker {
  private val periodTracker = PeriodTracker()

  private companion object {
    val LOG = logger<CompletionSelectionTrackerImpl>()
  }

  override fun getTotalTimeInSelection(): Long = periodTracker.totalTime(currentSelectionTime())
  override fun getTimesInSelection(): Int = periodTracker.count(currentSelectionTime())
  override fun getAverageTimeInSelection(): Double = periodTracker.average(currentSelectionTime())
  override fun getMaxTimeInSelection(): Long? = periodTracker.maxDuration(currentSelectionTime())
  override fun getMinTimeInSelection(): Long? = periodTracker.minDuration(currentSelectionTime())

  private var selectionStartedTimestamp: Long = -1

  fun itemSelected() {
    LOG.assertTrue(selectionStartedTimestamp == -1L, "Element already selected")
    selectionStartedTimestamp = System.currentTimeMillis()
  }

  fun itemUnselected() {
    val timestamp = selectionStartedTimestamp
    if (timestamp != -1L) {
      periodTracker.addDuration(System.currentTimeMillis() - timestamp)
      selectionStartedTimestamp = -1
    }
    else {
      LOG.error("Element should be selected")
    }
  }

  private fun currentSelectionTime(): Long? {
    val selectionStarted = selectionStartedTimestamp
    if (selectionStarted != -1L) {
      return System.currentTimeMillis() - selectionStarted
    }
    return null
  }
}
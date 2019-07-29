// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

class LookupSessionFactorsStorage(val startedTimestamp: Long) {
  private var visibleSize: Int = -1
  private var sortingCount: Int = 0

  val queryTracker: CompletionQueryTrackerImpl = CompletionQueryTrackerImpl(startedTimestamp)
  fun getVisibleSize(): Int = visibleSize
  fun getSortingOrder(): Int = sortingCount

  fun fireSortingPerforming(visibleSize: Int) {
    this.visibleSize = visibleSize
    sortingCount += 1
  }
}
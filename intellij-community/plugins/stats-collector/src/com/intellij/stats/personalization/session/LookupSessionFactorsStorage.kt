// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

class LookupSessionFactorsStorage(val startedTimestamp: Long) {
  private var visibleSize: Int = -1
  private var sortingCount: Int = 0

  private var factors: Map<String, String> = emptyMap()

  val queryTracker: CompletionQueryTrackerImpl = CompletionQueryTrackerImpl(startedTimestamp)
  fun getVisibleSize(): Int = visibleSize
  fun getSortingOrder(): Int = sortingCount

  fun fireSortingPerforming(visibleSize: Int) {
    this.visibleSize = visibleSize
    sortingCount += 1
  }

  fun updateLastUsedFactors(lookupFactors: Map<String, Any>) {
    factors = lookupFactors.mapValues { it.value.toString() }
  }

  fun getLastUsedCommonFactors(): Map<String, String> {
    return factors
  }
}
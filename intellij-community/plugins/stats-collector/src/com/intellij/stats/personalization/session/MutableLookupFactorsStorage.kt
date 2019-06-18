// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

class MutableLookupFactorsStorage(override val startedTimestamp: Long) : LookupFactorsStorage {
  private var visibleSize: Int = -1
  private var sortingCount: Int = 0
  override val queryTracker: CompletionQueryTrackerImpl = CompletionQueryTrackerImpl(startedTimestamp)

  private val item2storage: MutableMap<String, MutableElementFactorsStorage> = mutableMapOf()
  override fun getVisibleSize(): Int = visibleSize

  override fun getSortingOrder(): Int = sortingCount

  override fun getItemStorage(id: String): MutableElementFactorsStorage = item2storage.computeIfAbsent(id) {
    MutableElementFactorsStorage()
  }

  fun fireSortingPerforming(visibleSize: Int) {
    this.visibleSize = visibleSize
    sortingCount += 1
  }
}
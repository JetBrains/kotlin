// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

class CompletionQueryTrackerImpl(sessionStartedTimestamp: Long) : CompletionQueryTracker {
  private var prefixShiftCount = 0
  private var currentQuery: String = ""

  private var uniqueQueries: MutableMap<Int, MutableMap<String, Int>> = mutableMapOf()
  private var queriesCount = 0
  private var currentQueryFrequency: Int = 0

  override fun getCurrentQueryFrequency() = currentQueryFrequency

  override val durations: QueriesDuration = QueriesDuration(sessionStartedTimestamp)

  init {
    updateQuery("")
  }

  override fun getUniqueQueriesCount(): Int = uniqueQueries.values.sumBy { it.size }

  override fun getTotalQueriesCount(): Int = queriesCount

  fun afterAppend(c: Char) {
    updateQuery(currentQuery + c)
  }

  fun afterTruncate() {
    var query = currentQuery
    if (currentQuery.isEmpty()) {
      prefixShiftCount += 1
    }
    else {
      query = currentQuery.dropLast(1)
    }
    updateQuery(query)
  }

  private fun updateQuery(newQuery: String) {
    val prefixMap = uniqueQueries.computeIfAbsent(prefixShiftCount) { mutableMapOf() }
    currentQueryFrequency = prefixMap.getOrDefault(newQuery, 0) + 1
    prefixMap[newQuery] = currentQueryFrequency
    queriesCount += 1
    currentQuery = newQuery

    durations.fireQueryChanged()
  }
}
// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

interface CompletionQueryTracker {
  fun getUniqueQueriesCount(): Int
  fun getTotalQueriesCount(): Int
  fun getCurrentQueryFrequency(): Int

  val durations: Durations

  interface Durations {
    fun getCurrentQueryDuration(): Long
    fun getAverageQueryDuration(): Double
    fun getMinQueryDuration(): Long
    fun getMaxQueryDuration(): Long
  }
}

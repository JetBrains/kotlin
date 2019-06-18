// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

interface LookupFactorsStorage {
  fun getVisibleSize(): Int
  fun getSortingOrder(): Int
  fun getItemStorage(id: String): LookupElementFactorsStorage

  val startedTimestamp: Long
  val queryTracker: CompletionQueryTracker
}
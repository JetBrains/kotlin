// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.impl

import com.intellij.completion.ml.common.PrefixMatchingType
import com.intellij.stats.personalization.*

class PrefixMatchingTypeReader(private val factor: DailyAggregatedDoubleFactor) : FactorReader {
  fun getCompletionCountByType(type: PrefixMatchingType): Double =
    factor.aggregateSum().getOrDefault(type.toString(), 0.0)

  fun getTotalCompletionCount(): Double = factor.aggregateSum().values.sum()
}

class PrefixMatchingTypeUpdater(private val factor: MutableDoubleFactor) : FactorUpdater {
  fun fireCompletionPerformed(type: PrefixMatchingType) {
    factor.incrementOnToday(type.toString())
  }
}

class PrefixMatchingTypeRatio(private val type: PrefixMatchingType) : UserFactor {

  override val id: String = "PrefixMatchingTypeRatioOf$type"
  override fun compute(storage: UserFactorStorage): String? {
    val reader = storage.getFactorReader(UserFactorDescriptions.PREFIX_MATCHING_TYPE)
    val total = reader.getTotalCompletionCount()
    return if (total == 0.0) "0.0" else (reader.getCompletionCountByType(type) / total).toString()
  }
}
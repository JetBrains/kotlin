// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.impl.LookupUsageDescriptor
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.stats.storage.factors.LookupStorage

class MLRankingLookupUsageDescriptor : LookupUsageDescriptor {
  override fun getExtensionKey(): String = "ml"

  override fun fillUsageData(lookup: Lookup, usageData: FeatureUsageData) {
    if (lookup.isCompletion && lookup is LookupImpl) {
      val storage = LookupStorage.get(lookup)
      if (storage != null) {
        usageData.apply {
          addData("total_ml_time", storage.performanceTracker.totalMLTimeContribution())

          addData("ml_used", storage.mlUsed())
          addData("version", storage.model?.version() ?: "unknown")
        }
      }
    }
  }
}
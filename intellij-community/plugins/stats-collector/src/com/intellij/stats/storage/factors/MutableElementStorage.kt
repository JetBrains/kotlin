// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.storage.factors

import com.intellij.stats.personalization.session.ElementSessionFactorsStorage
import com.intellij.completion.sorting.FeatureUtils

class MutableElementStorage : LookupElementStorage {
  private var factors: Map<String, Any>? = null
  override fun getLastUsedFactors(): Map<String, Any>? {
    return factors
  }

  fun fireElementScored(factors: MutableMap<String, Any>, score: Double?) {
    if (score != null) {
      factors[FeatureUtils.ML_RANK] = score
    }

    this.factors = factors
  }

  override val sessionFactors: ElementSessionFactorsStorage = ElementSessionFactorsStorage()
}
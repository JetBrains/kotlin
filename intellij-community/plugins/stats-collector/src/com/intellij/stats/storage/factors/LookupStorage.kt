// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.storage.factors

import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.completion.sorting.RankingSupport
import com.intellij.lang.Language
import com.intellij.stats.personalization.session.LookupSessionFactorsStorage

interface LookupStorage {
  companion object {
    fun get(lookup: LookupImpl): LookupStorage? = MutableLookupStorage.get(lookup)
  }

  val model: RankingSupport.LanguageRanker?
  val language: Language
  val startedTimestamp: Long
  val sessionFactors: LookupSessionFactorsStorage
  val userFactors: Map<String, String?>
  val contextFactors: Map<String, String>
  fun getItemStorage(id: String): LookupElementStorage
}
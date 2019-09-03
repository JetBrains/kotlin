// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.storage.factors

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.completion.sorting.RankingModelWrapper
import com.intellij.completion.sorting.RankingSupport
import com.intellij.lang.Language
import com.intellij.openapi.util.Key
import com.intellij.stats.completion.idString
import com.intellij.stats.personalization.session.LookupSessionFactorsStorage

class MutableLookupStorage(
  override val startedTimestamp: Long,
  override val language: Language,
  override val model: RankingModelWrapper)
  : LookupStorage {
  override var userFactors: Map<String, String?> = emptyMap()
  override var contextFactors: Map<String, String> = emptyMap()

  companion object {
    private val LOOKUP_STORAGE = Key.create<MutableLookupStorage>("completion.ml.lookup.storage")

    fun get(lookup: LookupImpl): MutableLookupStorage? {
      return lookup.getUserData(LOOKUP_STORAGE)
    }

    fun initLookupStorage(lookup: LookupImpl, language: Language, startedTimestamp: Long): MutableLookupStorage {
      val storage = MutableLookupStorage(startedTimestamp, language, RankingSupport.getRankingModel(language))
      lookup.putUserData(LOOKUP_STORAGE, storage)
      return storage
    }
  }

  override val sessionFactors: LookupSessionFactorsStorage = LookupSessionFactorsStorage(startedTimestamp)

  private val item2storage: MutableMap<String, MutableElementStorage> = mutableMapOf()

  override fun getItemStorage(id: String): MutableElementStorage = item2storage.computeIfAbsent(id) {
    MutableElementStorage()
  }

  fun fireElementScored(element: LookupElement, factors: MutableMap<String, Any>, mlScore: Double?) {
    getItemStorage(element.idString()).fireElementScored(factors, mlScore)
  }
}
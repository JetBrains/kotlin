// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import com.intellij.stats.completion.idString

object SessionFactorsUtils {
  const val SESSION_FACTOR_PREFIX = "session_"
  private val LOOKUP_FACTORS_STORAGE = Key.create<MutableLookupFactorsStorage>("session.factors.storage.lookup")

  private val lookupFactors: List<SessionFactor.LookupBased> = listOf(
    lookupFactor("visible_size") { it.getVisibleSize() },
    lookupFactor("sorting_order") { it.getSortingOrder() },
    lookupFactor("duration") { System.currentTimeMillis() - it.startedTimestamp },
    lookupFactor("query_number") { it.queryTracker.getTotalQueriesCount() },
    lookupFactor("unique_queries") { it.queryTracker.getUniqueQueriesCount() },
    lookupFactor("current_query_frequency") { it.queryTracker.getCurrentQueryFrequency() },
    lookupFactor("current_query_duration") { it.queryTracker.durations.getCurrentQueryDuration() },
    lookupFactor("average_query_duration") { it.queryTracker.durations.getAverageQueryDuration() },
    lookupFactor("min_query_duration") { it.queryTracker.durations.getMinQueryDuration() },
    lookupFactor("max_query_duration") { it.queryTracker.durations.getMaxQueryDuration() }
  )

  private val elementFactors: List<SessionFactor.LookupElementBased> = listOf(
    elementFactor("visible_position") { it.getVisiblePosition() },
    elementFactor("times_in_selection") { it.selectionTracker.getTimesInSelection() },
    elementFactor("total_time_in_selection") { it.selectionTracker.getTotalTimeInSelection() },
    elementFactor("average_time_in_selection") { it.selectionTracker.getAverageTimeInSelection() },
    elementFactor("max_time_in_selection") { it.selectionTracker.getMaxTimeInSelection() },
    elementFactor("min_time_in_selection") { it.selectionTracker.getMinTimeInSelection() }
  )

  fun shouldUseSessionFactors(): Boolean = Registry.`is`("completion.stats.enable.session.factors")

  fun updateSessionFactors(lookup: LookupImpl, items: List<LookupElement>) {
    if (!shouldUseSessionFactors()) return
    val lookupStorage = getLookupFactorsStorage(lookup) ?: return
    lookupStorage.fireSortingPerforming(items.size)
    val lookupFactors = calculateLookupFactors(lookupStorage)
    items.forEachIndexed { i, item ->
      val storage = lookupStorage.getItemStorage(item.idString())
      storage.updateUsedSessionFactors(i, lookupFactors, calculateElementFactors(storage))
    }
  }

  fun saveSessionFactorsTo(map: MutableMap<String, Any>, lookup: LookupImpl, lookupElement: LookupElement) {
    val factorsStorage = getLookupFactorsStorage(lookup)?.getItemStorage(lookupElement.idString()) ?: return
    map.putAll(factorsStorage.lastUsedLookupFactors())
    map.putAll(factorsStorage.lastUsedElementFactors())
  }

  fun initLookupSessionFactors(lookup: LookupImpl, startedTimestamp: Long): MutableLookupFactorsStorage {
    val storage = MutableLookupFactorsStorage(startedTimestamp)
    lookup.putUserData(LOOKUP_FACTORS_STORAGE, storage)
    return storage
  }

  private fun getLookupFactorsStorage(lookup: LookupImpl): MutableLookupFactorsStorage? = lookup.getUserData(LOOKUP_FACTORS_STORAGE)

  private val SessionFactor.name: String
    get() = "$SESSION_FACTOR_PREFIX${this.simpleName}"

  private fun calculateLookupFactors(lookupStorage: LookupFactorsStorage): Map<String, Any> =
    calculateFactors(lookupStorage, lookupFactors) { factor, storage -> factor.getValue(storage) }

  private fun calculateElementFactors(elementStorage: LookupElementFactorsStorage): Map<String, Any> =
    calculateFactors(elementStorage, elementFactors) { factor, storage -> factor.getValue(storage) }

  private fun <S, F : SessionFactor> calculateFactors(storage: S, factors: Iterable<F>, valueExtractor: (F, S) -> Any?): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    for (factor in factors) {
      val factorName = factor.name
      assert(factorName !in result)
      val factorValue = valueExtractor(factor, storage)
      if (factorValue != null) {
        result[factorName] = factorValue
      }
    }

    return result
  }

  private class SessionLookupFactor(override val simpleName: String,
                                    private val valueExtractor: (LookupFactorsStorage) -> Any?) : SessionFactor.LookupBased {
    override fun getValue(storage: LookupFactorsStorage): Any? = valueExtractor(storage)
  }

  private fun lookupFactor(name: String, extractor: (LookupFactorsStorage) -> Any?): SessionFactor.LookupBased {
    return SessionLookupFactor(name, extractor)
  }

  private class ElementFactor(override val simpleName: String,
                              private val valueExtractor: (LookupElementFactorsStorage) -> Any?) : SessionFactor.LookupElementBased {
    override fun getValue(storage: LookupElementFactorsStorage): Any? = valueExtractor(storage)
  }

  private fun elementFactor(name: String, extractor: (LookupElementFactorsStorage) -> Any?): SessionFactor.LookupElementBased {
    return ElementFactor(name, extractor)
  }
}
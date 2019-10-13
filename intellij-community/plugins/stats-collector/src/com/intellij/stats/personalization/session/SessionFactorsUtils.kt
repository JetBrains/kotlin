// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.registry.Registry
import com.intellij.stats.completion.idString
import com.intellij.stats.storage.factors.LookupStorage
import com.intellij.stats.storage.factors.MutableLookupStorage

object SessionFactorsUtils {
  const val SESSION_FACTOR_PREFIX = "session_"

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

  fun updateSessionFactors(lookupStorage: MutableLookupStorage, items: List<LookupElement>): Map<String, Any> {
    if (!shouldUseSessionFactors()) return emptyMap()
    val sessionFactors = lookupStorage.sessionFactors
    sessionFactors.fireSortingPerforming(items.size)
    val lookupFactors = calculateLookupFactors(sessionFactors)
    lookupStorage.sessionFactors.updateLastUsedFactors(lookupFactors)
    items.forEachIndexed { i, item ->
      val storage = lookupStorage.getItemStorage(item.idString())
      storage.sessionFactors.updateUsedSessionFactors(i, calculateElementFactors(storage.sessionFactors))
    }

    return lookupFactors
  }

  fun saveElementFactorsTo(map: MutableMap<String, Any>, lookupStorage: LookupStorage, lookupElement: LookupElement) {
    val factorsStorage = lookupStorage.getItemStorage(lookupElement.idString()).sessionFactors
    map.putAll(factorsStorage.lastUsedElementFactors())
  }

  private val SessionFactor<*>.name: String
    get() = "$SESSION_FACTOR_PREFIX${this.simpleName}"

  private fun calculateLookupFactors(lookupStorage: LookupSessionFactorsStorage): Map<String, Any> =
    calculateFactors(lookupStorage, lookupFactors)

  private fun calculateElementFactors(elementStorage: ElementSessionFactorsStorage): Map<String, Any> =
    calculateFactors(elementStorage, elementFactors)

  private fun <S> calculateFactors(storage: S, factors: Iterable<SessionFactor<S>>): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    for (factor in factors) {
      val factorName = factor.name
      assert(factorName !in result)
      val factorValue = factor.getValue(storage)
      if (factorValue != null) {
        result[factorName] = factorValue
      }
    }

    return result
  }

  private class SessionLookupFactor(override val simpleName: String,
                                    private val valueExtractor: (LookupSessionFactorsStorage) -> Any?) : SessionFactor.LookupBased {
    override fun getValue(storage: LookupSessionFactorsStorage): Any? = valueExtractor(storage)
  }

  private fun lookupFactor(name: String, extractor: (LookupSessionFactorsStorage) -> Any?): SessionFactor.LookupBased {
    return SessionLookupFactor(name, extractor)
  }

  private class ElementFactor(override val simpleName: String,
                              private val valueExtractor: (ElementSessionFactorsStorage) -> Any?) : SessionFactor.LookupElementBased {
    override fun getValue(storage: ElementSessionFactorsStorage): Any? = valueExtractor(storage)
  }

  private fun elementFactor(name: String, extractor: (ElementSessionFactorsStorage) -> Any?): SessionFactor.LookupElementBased {
    return ElementFactor(name, extractor)
  }

  private interface SessionFactor<T> {
    val simpleName: String

    fun getValue(storage: T): Any?

    interface LookupBased : SessionFactor<LookupSessionFactorsStorage>
    interface LookupElementBased : SessionFactor<ElementSessionFactorsStorage>
  }

}
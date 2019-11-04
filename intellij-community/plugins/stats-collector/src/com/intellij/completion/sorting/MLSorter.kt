// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.completion.sorting

import com.intellij.codeInsight.completion.CompletionFinalSorter
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.registry.Registry
import com.intellij.stats.PerformanceTracker
import com.intellij.stats.completion.ItemsDiffCustomizingContributor
import com.intellij.stats.completion.RelevanceUtil
import com.intellij.stats.completion.prefixLength
import com.intellij.stats.personalization.session.SessionFactorsUtils
import com.intellij.stats.storage.factors.MutableLookupStorage
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Suppress("DEPRECATION")
class MLSorterFactory : CompletionFinalSorter.Factory {
  override fun newSorter() = MLSorter()
}


class MLSorter : CompletionFinalSorter() {
  private companion object {
    private val LOG = Logger.getInstance("#com.intellij.completion.sorting.MLSorter")
    private const val REORDER_ONLY_TOP_K = 7
  }

  private val cachedScore: MutableMap<LookupElement, ItemRankInfo> = IdentityHashMap()
  private val reorderOnlyTopItems: Boolean = Registry.`is`("completion.ml.reorder.only.top.items", true)

  override fun getRelevanceObjects(items: MutableIterable<LookupElement>): Map<LookupElement, List<Pair<String, Any>>> {
    if (cachedScore.isEmpty()) {
      return items.associate { it to listOf(Pair.create(FeatureUtils.ML_RANK, FeatureUtils.NONE as Any)) }
    }

    if (hasUnknownFeatures(items)) {
      return items.associate { it to listOf(Pair.create(FeatureUtils.ML_RANK, FeatureUtils.UNDEFINED as Any)) }
    }

    if (!isCacheValid(items)) {
      return items.associate { it to listOf(Pair.create(FeatureUtils.ML_RANK, FeatureUtils.INVALID_CACHE as Any)) }
    }

    return items.associate {
      val result = mutableListOf<Pair<String, Any>>()
      val cached = cachedScore[it]
      if (cached != null) {
        result.add(Pair.create(FeatureUtils.ML_RANK, cached.mlRank))
        result.add(Pair.create(FeatureUtils.BEFORE_ORDER, cached.positionBefore))
      }
      it to result
    }
  }

  private fun isCacheValid(items: Iterable<LookupElement>): Boolean {
    return items.map { cachedScore[it]?.prefixLength }.toSet().size == 1
  }

  private fun hasUnknownFeatures(items: Iterable<LookupElement>) = items.any {
    val score = cachedScore[it]
    score?.mlRank == null
  }

  override fun sort(items: MutableIterable<LookupElement>, parameters: CompletionParameters): Iterable<LookupElement?> {
    val lookup = LookupManager.getActiveLookup(parameters.editor) as? LookupImpl ?: return items
    val lookupStorage = MutableLookupStorage.get(lookup) ?: return items
    // Do nothing if unable to reorder items or to log the weights
    if (!lookupStorage.shouldComputeFeatures()) return items
    val startedTimestamp = System.currentTimeMillis()
    val prefixLength = lookup.prefixLength()

    val element2score = mutableMapOf<LookupElement, Double?>()
    val elements = items.toList()

    val positionsBefore = elements.withIndex().associate { it.value to it.index }

    tryFillFromCache(element2score, elements, prefixLength)
    val itemsForScoring = if (element2score.size == elements.size) emptyList() else elements
    calculateScores(element2score, itemsForScoring, positionsBefore,
                    prefixLength, lookup, lookupStorage, parameters)
    val finalRanking = sortByMlScores(elements, element2score, positionsBefore, lookupStorage)

    lookupStorage.performanceTracker.sortingPerformed(itemsForScoring.size, System.currentTimeMillis() - startedTimestamp)

    return finalRanking
  }

  private fun tryFillFromCache(element2score: MutableMap<LookupElement, Double?>,
                               items: List<LookupElement>,
                               prefixLength: Int) {
    for ((position, element) in items.withIndex()) {
      val cachedInfo = getCachedRankInfo(element, prefixLength, position)
      if (cachedInfo == null) return
      element2score[element] = cachedInfo.mlRank
    }
  }

  private fun calculateScores(element2score: MutableMap<LookupElement, Double?>,
                              items: List<LookupElement>,
                              positionsBefore: Map<LookupElement, Int>,
                              prefixLength: Int,
                              lookup: LookupImpl,
                              lookupStorage: MutableLookupStorage,
                              parameters: CompletionParameters) {
    if (items.isEmpty()) return

    val rankingModel = lookupStorage.model

    val commonSessionFactors = SessionFactorsUtils.updateSessionFactors(lookupStorage, items)
    val contextFactors = lookupStorage.contextFactors
    val features = RankingFeatures(lookupStorage.userFactors, contextFactors, commonSessionFactors)
    val relevanceObjects = lookup.getRelevanceObjects(items, false)
    val tracker = ModelTimeTracker()
    for (element in items) {
      val position = positionsBefore.getValue(element)
      val (relevance, additional) = RelevanceUtil.asRelevanceMaps(relevanceObjects.getOrDefault(element, emptyList()))
      SessionFactorsUtils.saveElementFactorsTo(additional, lookupStorage, element)
      calculateAdditionalFeaturesTo(additional, element, prefixLength, position, parameters)
      val score = when {
        rankingModel != null -> tracker.measure {
          calculateElementScore(rankingModel, element, position, features.withElementFeatures(relevance, additional), prefixLength)
        }
        else -> null
      }
      element2score[element] = score

      additional.putAll(relevance)
      lookupStorage.fireElementScored(element, additional, score)
    }

    tracker.finished(lookupStorage.performanceTracker)
  }

  private fun sortByMlScores(items: List<LookupElement>,
                             element2score: Map<LookupElement, Double?>,
                             positionsBefore: Map<LookupElement, Int>,
                             lookupStorage: MutableLookupStorage): Iterable<LookupElement> {
    val mlScoresUsed = element2score.values.none { it == null }
    if (LOG.isDebugEnabled) {
      LOG.debug("ML sorting in completion used=$mlScoresUsed for language=${lookupStorage.language.id}")
    }

    if (mlScoresUsed) {
      lookupStorage.fireReorderedUsingMLScores()
      val topItemsCount = if (reorderOnlyTopItems) REORDER_ONLY_TOP_K else Int.MAX_VALUE
      return items.reorderByMLScores(element2score, topItemsCount).addDiagnosticsIfNeeded(positionsBefore, topItemsCount)
    }

    return items
  }

  private fun calculateAdditionalFeaturesTo(
    additionalMap: MutableMap<String, Any>,
    lookupElement: LookupElement,
    prefixLength: Int,
    position: Int,
    parameters: CompletionParameters) {

    additionalMap["position"] = position
    additionalMap["query_length"] = prefixLength
    additionalMap["result_length"] = lookupElement.lookupString.length
    additionalMap["auto_popup"] = parameters.isAutoPopup
    additionalMap["completion_type"] = parameters.completionType.toString()
    additionalMap["invocation_count"] = parameters.invocationCount
  }

  private fun Iterable<LookupElement>.reorderByMLScores(element2score: Map<LookupElement, Double?>, toReorder: Int): Iterable<LookupElement> {
    val result = this.sortedByDescending { element2score.getValue(it) }.take(toReorder).toCollection(linkedSetOf())
    result.addAll(this)
    return result
  }

  private fun Iterable<LookupElement>.addDiagnosticsIfNeeded(positionsBefore: Map<LookupElement, Int>, reordered: Int): Iterable<LookupElement> {
    if (Registry.`is`("completion.stats.show.ml.ranking.diff")) {
      this.forEachIndexed { position, element ->
        val before = positionsBefore.getValue(element)
        if (before < reordered || position < reordered) {
          element.updateDiffValue(position - before)
        }
      }
    }

    return this
  }

  private fun getCachedRankInfo(element: LookupElement, prefixLength: Int, position: Int): ItemRankInfo? {
    val cached = cachedScore[element]
    if (cached != null && prefixLength == cached.prefixLength && cached.positionBefore == position) {
      return cached
    }
    return null
  }

  /**
   * Null means we encountered unknown features and are unable to score
   */
  private fun calculateElementScore(ranker: RankingModelWrapper,
                                    element: LookupElement,
                                    position: Int,
                                    features: RankingFeatures,
                                    prefixLength: Int): Double? {
    val mlRank: Double? = if (ranker.canScore(features)) ranker.score(features) else null
    val info = ItemRankInfo(position, mlRank, prefixLength)
    cachedScore[element] = info

    return info.mlRank
  }

  private fun LookupElement.updateDiffValue(newValue: Int) {
    val diff = getUserData(ItemsDiffCustomizingContributor.DIFF_KEY) ?: AtomicInteger()
      .apply { putUserData(ItemsDiffCustomizingContributor.DIFF_KEY, this) }

    diff.set(newValue)
  }

  /*
   * Measures time on getting predictions from the ML model
   */
  private class ModelTimeTracker {
    private var itemsScored: Int = 0
    private var timeSpent: Long = 0L
    fun measure(scoringFun: () -> Double?): Double? {
      val start = System.nanoTime()
      val result = scoringFun.invoke()
      if (result != null) {
        itemsScored += 1
        timeSpent += System.nanoTime() - start
      }

      return result
    }

    fun finished(performanceTracker: PerformanceTracker) {
      if (itemsScored != 0) {
        performanceTracker.itemsScored(itemsScored, TimeUnit.NANOSECONDS.toMillis(timeSpent))
      }
    }
  }
}

private data class ItemRankInfo(val positionBefore: Int, val mlRank: Double?, val prefixLength: Int)

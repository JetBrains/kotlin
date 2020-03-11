// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionService
import com.intellij.codeInsight.completion.CompletionSorter
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.internal.statistic.utils.getPluginInfo
import com.intellij.psi.ForceableComparable
import com.intellij.psi.WeighingService

object MLFeaturesUtil {
  fun valueAsString(featureValue: MLFeatureValue): String {
    return when (featureValue) {
      is MLFeatureValue.BinaryValue -> if (featureValue.value) "1" else "0"
      is MLFeatureValue.FloatValue -> featureValue.value.toString()
      is MLFeatureValue.CategoricalValue -> featureValue.value
      is MLFeatureValue.ClassNameValue -> getClassNameSafe(featureValue)
    }
  }

  val classNameSafeCache = CacheBuilder
    .newBuilder()
    .softValues()
    .maximumSize(100)
    .build(object: CacheLoader<Class<*>, String>() {
      override fun load(clazz: Class<*>) = if (getPluginInfo(clazz).isSafeToReport()) clazz.name else "third.party"
    })

  private fun getClassNameSafe(feature: MLFeatureValue.ClassNameValue): String {
    val clazz = feature.value
    return classNameSafeCache[clazz]
  }

  fun addWeighersToNonDefaultSorter(sorter: CompletionSorter, location: CompletionLocation, vararg weigherIds: String): CompletionSorter {
      // from BaseCompletionService.defaultSorter
    var result = sorter
    for (weigher in WeighingService.getWeighers(CompletionService.RELEVANCE_KEY)) {
      val id = weigher.toString()
      if (weigherIds.contains(id)) {
        result = result.weigh(object : LookupElementWeigher(id, true, false) {
          override fun weigh(element: LookupElement): Comparable<*>? {
            val weigh = weigher.weigh(element, location) ?: return DummyWeigherComparableDelegate.EMPTY
            return DummyWeigherComparableDelegate(weigh)
          }
        })
      }
    }
    return result
  }
}

private class DummyWeigherComparableDelegate(private val weigh: Comparable<*>?)
  : Comparable<DummyWeigherComparableDelegate>, ForceableComparable {

  companion object {
    val EMPTY = DummyWeigherComparableDelegate(null)
  }

  override fun force() {
    if (weigh is ForceableComparable) {
      (weigh as ForceableComparable).force()
    }
  }

  override operator fun compareTo(other: DummyWeigherComparableDelegate): Int {
    return 0
  }

  override fun toString(): String {
    return weigh?.toString() ?: ""
  }
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionWeigher
import com.intellij.codeInsight.completion.ml.ElementFeatureProvider
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.stats.storage.factors.LookupStorage

class MLCompletionWeigher : CompletionWeigher() {
  override fun weigh(element: LookupElement, location: CompletionLocation): Comparable<Nothing>? {
    val storage = (LookupManager.getActiveLookup(location.completionParameters.editor) as? LookupImpl)
                    ?.let { LookupStorage.get(it) } ?: return DummyComparable.EMPTY
    if (!storage.shouldComputeFeatures()) return DummyComparable.EMPTY
    val result = mutableMapOf<String, Any>()
    val contextFeatures = storage.contextProvidersResult()
    for (provider in ElementFeatureProvider.forLanguage(storage.language)) {
      val name = provider.name
      for ((featureName, featureValue) in provider.calculateFeatures(element, location, contextFeatures)) {
        result["${name}_$featureName"] = featureValue
      }
    }

    return if (result.isEmpty()) DummyComparable.EMPTY else DummyComparable(result)
  }

  private class DummyComparable(values: Map<String, Any>) : Comparable<Any> {
    val representation = calculateRepresentation(values)

    override fun compareTo(other: Any): Int = 0

    override fun toString(): String = representation

    companion object {
      val EMPTY = DummyComparable(emptyMap())

      private fun calculateRepresentation(values: Map<String, Any>): String {
        return values.entries.joinToString(",", "[", "]", transform = { "${it.key}=${it.value}" })
      }
    }
  }
}
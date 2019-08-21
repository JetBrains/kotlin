// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionWeigher
import com.intellij.codeInsight.lookup.LookupElement

class MLCompletionWeigher : CompletionWeigher() {
  override fun weigh(element: LookupElement, location: CompletionLocation): Comparable<Nothing>? {
    val psiFile = location.completionParameters.originalFile
    val contextFeatures = ContextFeatures.extract(psiFile)
    val result = mutableMapOf<String, Any>()
    for (provider in ElementFeatureProvider.forLanguage(psiFile.language)) {
      val name = provider.name
      for ((featureName, featureValue) in provider.calculateFeatures(element, location, contextFeatures)) {
        result["${name}_$featureName"] = featureValue
      }
    }

    return if (result.isEmpty()) null else DummyComparable(result)
  }

  private class DummyComparable(values: Map<String, Any>) : Comparable<Any> {
    val representation = calculateRepresentation(values)

    override fun compareTo(other: Any): Int = 0

    override fun toString(): String = representation

    private companion object {
      private fun calculateRepresentation(values: Map<String, Any>): String {
        return values.entries.joinToString(",", "[", "]", transform = { "${it.key}=${it.value}" })
      }
    }
  }
}
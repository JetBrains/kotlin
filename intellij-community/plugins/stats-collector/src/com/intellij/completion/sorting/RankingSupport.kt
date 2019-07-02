// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.sorting

import com.intellij.lang.Language
import com.intellij.stats.personalization.session.SessionFactorsUtils
import com.jetbrains.completion.feature.Feature
import com.jetbrains.completion.feature.ModelMetadataEx
import com.jetbrains.completion.feature.impl.FeatureTransformer
import com.jetbrains.completion.ranker.JavaCompletionRanker
import com.jetbrains.completion.ranker.KotlinCompletionRanker
import com.jetbrains.completion.ranker.LanguageCompletionRanker
import com.jetbrains.completion.ranker.PythonCompletionRanker


object RankingSupport {
  private val language2ranker: Map<String, LanguageRanker> = buildRankerMap()

  fun getRanker(language: Language?): LanguageRanker? {
    if (language == null) return null
    return language2ranker[language.key()]
  }

  fun availableLanguages(): List<String> {
    return language2ranker.values.map { it.displayName }
  }

  private fun Language.key(): String = displayName.toLowerCase()

  class LanguageRanker(val displayName: String, private val ranker: LanguageCompletionRanker) {
    private val transformer: FeatureTransformer = ranker.modelMetadata.createTransformer()
    private val useSessionFactors: Boolean = ranker.modelMetadata.hasSessionFactors()

    fun rank(relevance: Map<String, Any>, userFactors: Map<String, Any?>): Double {
      return ranker.rank(transformer.featureArray(relevance, userFactors))
    }

    fun unknownFeatures(features: Set<String>): List<String> = ranker.modelMetadata.unknownFeatures(features)

    fun version(): String? {
      return ranker.modelMetadata.version
    }

    fun useSessionFeatures(): Boolean = useSessionFactors

    private fun ModelMetadataEx.hasSessionFactors(): Boolean {
      fun isSessionFeature(feature: Feature) = feature.name.startsWith(SessionFactorsUtils.SESSION_FACTOR_PREFIX)
      return float.any { isSessionFeature(it) } || binary.any { isSessionFeature(it) } || categorical.any { isSessionFeature(it) }
    }
  }

  private fun buildRankerMap(): Map<String, LanguageRanker> {
    return mapOf(
      "java" to LanguageRanker("Java", JavaCompletionRanker()),
      "kotlin" to LanguageRanker("Kotlin", KotlinCompletionRanker()),
      "python" to LanguageRanker("Python", PythonCompletionRanker())
    )
  }
}
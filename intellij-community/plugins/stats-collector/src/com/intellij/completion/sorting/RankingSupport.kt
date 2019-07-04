// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.sorting

import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.completion.feature.impl.FeatureTransformer
import com.jetbrains.completion.ranker.JavaCompletionRanker
import com.jetbrains.completion.ranker.KotlinCompletionRanker
import com.jetbrains.completion.ranker.LanguageCompletionRanker
import com.jetbrains.completion.ranker.PythonCompletionRanker
import java.io.IOException


object RankingSupport {
  private val LOG = logger<RankingSupport>()
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

    fun rank(relevance: Map<String, Any>, userFactors: Map<String, Any?>): Double {
      return ranker.rank(transformer.featureArray(relevance, userFactors))
    }

    fun unknownFeatures(features: Set<String>): List<String> = ranker.modelMetadata.unknownFeatures(features)

    fun version(): String? {
      return ranker.modelMetadata.version
    }
  }

  private fun registerRanker(rankerMap: MutableMap<String, LanguageRanker>, builder: () -> LanguageRanker) {
    try {
      val ranker = builder()
      rankerMap[ranker.displayName.toLowerCase()] = ranker
    }
    catch (e: IOException) {
      LOG.error("Could not initialize language ranker", e)
    }
  }

  private fun buildRankerMap(): Map<String, LanguageRanker> {
    val result = mutableMapOf<String, LanguageRanker>()

    registerRanker(result) { LanguageRanker("Java", JavaCompletionRanker()) }
    registerRanker(result) { LanguageRanker("Kotlin", KotlinCompletionRanker()) }
    registerRanker(result) { LanguageRanker("Python", PythonCompletionRanker()) }

    return result
  }
}
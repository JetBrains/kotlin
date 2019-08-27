// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.sorting

import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.logger
import com.intellij.stats.personalization.session.SessionFactorsUtils
import com.jetbrains.completion.feature.Feature
import com.jetbrains.completion.feature.ModelMetadataEx
import com.jetbrains.completion.feature.impl.FeatureTransformer
import com.jetbrains.completion.ranker.JavaCompletionRanker
import com.jetbrains.completion.ranker.KotlinCompletionRanker
import com.jetbrains.completion.ranker.LanguageCompletionRanker
import com.jetbrains.completion.ranker.PythonCompletionRanker


object RankingSupport {
  private val LOG = logger<RankingSupport>()
  private val language2rankerFactory: Map<String, () -> LanguageCompletionRanker> = mapOf(
    "Java" to { JavaCompletionRanker() },
    "Kotlin" to { KotlinCompletionRanker() },
    "Python" to { PythonCompletionRanker() }
  )
  private val language2rankerCache: MutableMap<String, LanguageRanker?> = mutableMapOf()

  fun getRanker(language: Language?): LanguageRanker? {
    if (language == null || language.displayName !in language2rankerFactory.keys) return null
    val name = language.displayName
    if (language2rankerCache.containsKey(name)) return language2rankerCache[name]

    val rankerFactory = language2rankerFactory[name]
    val ranker = if (rankerFactory == null) null
    else rankerFactory.tryBuild(name)?.let { LanguageRanker(name, it) }

    language2rankerCache[name] = ranker
    return ranker
  }

  fun availableLanguages(): List<String> {
    return language2rankerFactory.keys.toList()
  }

  private fun (() -> LanguageCompletionRanker).tryBuild(name: String): LanguageCompletionRanker? {
    try {
      return this.invoke()
    }
    catch (e: Exception) {
      LOG.error("Failed to initialize '$name' ranker", e)
    }
    return null
  }

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
}
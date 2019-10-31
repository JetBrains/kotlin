// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.completion.ranker

import com.intellij.internal.ml.completion.RankingModelProvider
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import kotlin.streams.asSequence

/*
 * Utility interface for {@link com.intellij.internal.ml.completion.RankingModelProvider}. Allows shipping models for different languages
 * with statsCollector plugin without conflicts with models shipped with language-specific plugins like Scala and Kotlin. This mechanism
 * allows us to deliver new versions of models and evaluate them more often. There could be many instances of WeakModelProvider but
 * maximum 1 RankingModelProvider which does not implement WeakModelProvider
 */
@ApiStatus.Internal
interface WeakModelProvider : RankingModelProvider {
  /*
   * If returns true, than all other providers should be skipped and this one will be used
   */
  fun shouldReplace(): Boolean = false

  /*
   * Checks that provider can be used in current environment
   */
  fun canBeUsed(): Boolean

  companion object {
    private val EP_NAME: ExtensionPointName<RankingModelProvider> = ExtensionPointName("com.intellij.completion.ml.model")

    @JvmStatic
    fun findProvider(language: Language): RankingModelProvider? {
      val (weakProviders, strongProviders) = availableProviders()
        .filter { it.isLanguageSupported(language) }
        .partition { it is WeakModelProvider }

      check(strongProviders.size <= 1) { "Too many strong providers: $strongProviders" }

      val strongProvider = strongProviders.singleOrNull()
      if (weakProviders.isEmpty()) return strongProvider

      // all weak providers are fallback by default
      val fallbackProviders = weakProviders.filterIsInstance<WeakModelProvider>().filter { it.canBeUsed() }

      // one of weak providers can be replace provider
      val replaceProviders = fallbackProviders.filter { it.shouldReplace() }

      check(replaceProviders.size <= 1) { "Too many weak provider can replace the strong one: $replaceProviders" }
      if (strongProvider != null) {
        val replaceProvider = replaceProviders.singleOrNull()
        return replaceProvider ?: strongProvider
      }
      else {
        if (replaceProviders.isNotEmpty()) {
          return replaceProviders.single()
        }
        else {
          check(fallbackProviders.size <= 1) { "Too many fallback providers: $fallbackProviders" }
          return fallbackProviders.single()
        }
      }
    }

    fun availableProviders(): Sequence<RankingModelProvider> {
      return EP_NAME.extensions().asSequence().filter { it !is WeakModelProvider || it.canBeUsed() }
    }

    @TestOnly
    fun registerProvider(provider: RankingModelProvider, parentDisposable: Disposable) {
      val extensionPoint = Extensions.getRootArea().getExtensionPoint(EP_NAME)
      extensionPoint.registerExtension(provider, parentDisposable)
    }
  }
}

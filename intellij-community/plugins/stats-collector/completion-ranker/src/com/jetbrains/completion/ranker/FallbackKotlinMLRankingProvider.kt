// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.completion.ranker

import com.completion.ranker.model.kotlin.MLWhiteBox
import com.intellij.ide.plugins.PluginManager
import com.intellij.internal.ml.DecisionFunction
import com.intellij.internal.ml.ModelMetadata
import com.intellij.internal.ml.completion.CompletionRankingModelBase
import com.intellij.internal.ml.completion.JarCompletionModelProvider
import com.intellij.lang.Language
import com.intellij.openapi.extensions.PluginId

class FallbackKotlinMLRankingProvider : JarCompletionModelProvider("Kotlin", "kotlin_features"), WeakModelProvider {
  override fun createModel(metadata: ModelMetadata): DecisionFunction {
    return object : CompletionRankingModelBase(metadata) {
      override fun predict(features: DoubleArray?): Double = MLWhiteBox.makePredict(features)
    }
  }

  override fun isLanguageSupported(language: Language): Boolean = language.id.compareTo("kotlin", ignoreCase = true) == 0

  override fun canBeUsed(): Boolean {
    return PluginManager.getInstance().findEnabledPlugin(PluginId.findId(KOTLIN_PLUGIN_ID) ?: return false)?.isEnabled ?: false
  }

  override fun shouldReplace(): Boolean = false

  private companion object {
    private const val KOTLIN_PLUGIN_ID = "org.jetbrains.kotlin"
  }
}
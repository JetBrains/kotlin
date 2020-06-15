// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.completion.ranker

import com.intellij.ide.plugins.PluginManager
import com.intellij.internal.ml.DecisionFunction
import com.intellij.internal.ml.ModelMetadata
import com.intellij.internal.ml.completion.CompletionRankingModelBase
import com.intellij.internal.ml.completion.JarCompletionModelProvider
import com.intellij.lang.Language
import com.intellij.openapi.extensions.PluginId
import com.jetbrains.completion.ranker.model.scala.MLGlassBox

class FallbackScalaMLRankingProvider : JarCompletionModelProvider("Scala", "scala_features"), WeakModelProvider {
  override fun createModel(metadata: ModelMetadata): DecisionFunction {
    return object : CompletionRankingModelBase(metadata) {
      override fun predict(features: DoubleArray?): Double = MLGlassBox.makePredict(features)
    }
  }

  override fun isLanguageSupported(language: Language): Boolean = language.id.compareTo("Scala", ignoreCase = true) == 0

  override fun canBeUsed(): Boolean {
    return PluginManager.getInstance().findEnabledPlugin(PluginId.findId(SCALA_PLUGIN_ID) ?: return false)?.isEnabled ?: false
  }

  override fun shouldReplace(): Boolean = false

  private companion object {
    private const val SCALA_PLUGIN_ID = "org.intellij.scala"
  }
}
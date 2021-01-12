// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.sorting

import com.intellij.internal.ml.DecisionFunction

class LanguageRankingModel(private val model: DecisionFunction) : RankingModelWrapper {
  private val featuresArrayBuilder = CachingFeaturesArrayBuilder(model.featuresOrder)
  override fun canScore(features: RankingFeatures): Boolean {
    return model.requiredFeatures.all { features.hasFeature(it) }
           && model.getUnknownFeatures(features.relevanceFeatures()).isEmpty()
  }

  override fun version(): String? = model.version()

  override fun score(features: RankingFeatures): Double? {
    return model.predict(featuresArrayBuilder.buildArray(features))
  }
}

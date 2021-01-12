// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.sorting

import com.intellij.internal.ml.FeatureMapper

class CachingFeaturesArrayBuilder(private val featuresOrder: Array<FeatureMapper>) {
  private val array = DoubleArray(featuresOrder.size)
  fun buildArray(features: RankingFeatures): DoubleArray {
    for (i in featuresOrder.indices) {
      val mapper = featuresOrder[i]
      val value = features.featureValue(mapper.featureName)
      array[i] = mapper.asArrayValue(value)
    }

    return array
  }
}

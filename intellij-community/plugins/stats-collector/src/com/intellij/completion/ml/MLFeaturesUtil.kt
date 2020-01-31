// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml

import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.internal.statistic.utils.getPluginInfo

object MLFeaturesUtil {
  fun valueAsString(featureValue: MLFeatureValue): String {
    return when (featureValue) {
      is MLFeatureValue.BinaryValue -> if (featureValue.value) "1" else "0"
      is MLFeatureValue.FloatValue -> featureValue.value.toString()
      is MLFeatureValue.CategoricalValue -> featureValue.value
      is MLFeatureValue.ClassNameValue -> getClassNameSafe(featureValue)
    }
  }

  private fun getClassNameSafe(feature: MLFeatureValue.ClassNameValue): String {
    val clazz = feature.value
    return if (getPluginInfo(clazz).isSafeToReport()) clazz.name else "third.party"
  }
}
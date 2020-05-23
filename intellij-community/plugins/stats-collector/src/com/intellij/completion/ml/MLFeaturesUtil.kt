// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
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

  private data class ClassNames(val simpleName: String, val fullName: String)

  private fun Class<*>.getNames(): ClassNames {
    return ClassNames(simpleName, name)
  }

  private val THIRD_PARTY_NAME = ClassNames("third.party", "third.party")

  private val CLASS_NAMES_CACHE: Cache<String, ClassNames> = CacheBuilder
    .newBuilder()
    .maximumSize(100)
    .build()

  private fun getClassNameSafe(feature: MLFeatureValue.ClassNameValue): String {
    val clazz = feature.value
    val names = CLASS_NAMES_CACHE.get(clazz.name) { if (getPluginInfo(clazz).isSafeToReport()) clazz.getNames() else THIRD_PARTY_NAME }
    return if (feature.useSimpleName) names.simpleName else names.fullName
  }
}

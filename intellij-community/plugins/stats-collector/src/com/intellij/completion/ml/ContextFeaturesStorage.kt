// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml

import com.intellij.codeInsight.completion.ml.ContextFeatures
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.openapi.util.UserDataHolderBase

class ContextFeaturesStorage(private val featuresSnapshot: Map<String, MLFeatureValue>) : ContextFeatures, UserDataHolderBase() {
  companion object {
    val EMPTY = ContextFeaturesStorage(emptyMap())
  }

  override fun binaryValue(name: String): Boolean? = (featuresSnapshot[name])?.asBinary()

  override fun floatValue(name: String): Double? = (featuresSnapshot[name])?.asFloat()

  override fun categoricalValue(name: String): String? = (featuresSnapshot[name])?.asCategorical()

  override fun asMap(): Map<String, String> = featuresSnapshot.mapValues { it.value.toString() }
}


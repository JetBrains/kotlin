// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml

import com.intellij.codeInsight.completion.ml.ContextFeatures
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import java.util.*

class ContextFeaturesStorage(private val featuresSnapshot: Map<String, MLFeatureValue>): ContextFeatures {
  companion object {
    private val CONTEXT_FEATURES_KEY = Key.create<ContextFeaturesStorage>("com.intellij.completion.ml.context_features")
    private val EMPTY = ContextFeaturesStorage(emptyMap())

    fun extract(element: PsiElement): ContextFeaturesStorage {
      return element.getUserData(CONTEXT_FEATURES_KEY) ?: EMPTY
    }

    fun setContextFeatures(element: PsiElement, features: Map<String, MLFeatureValue>) {
      element.putUserData(CONTEXT_FEATURES_KEY,
                          ContextFeaturesStorage(Collections.unmodifiableMap(features)))
    }

    fun clear(element: PsiElement) {
      element.putUserData(CONTEXT_FEATURES_KEY, null)
    }
  }

  override fun binaryValue(name: String): Boolean? = (featuresSnapshot[name])?.asBinary()

  override fun floatValue(name: String): Double? = (featuresSnapshot[name])?.asFloat()

  override fun categoricalValue(name: String): String? = (featuresSnapshot[name])?.asCategorical()
}


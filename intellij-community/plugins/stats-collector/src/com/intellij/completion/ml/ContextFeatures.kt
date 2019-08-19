// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import java.util.*

class ContextFeatures(private val featuresSnapshot: Map<String, MLFeatureValue>) {
  companion object {
    private val CONTEXT_FEATURES_KEY = Key.create<ContextFeatures>("com.intellij.completion.ml.context_features")
    private val EMPTY = ContextFeatures(emptyMap())

    fun extract(element: PsiElement): ContextFeatures {
      return element.getUserData(CONTEXT_FEATURES_KEY) ?: EMPTY
    }

    fun setContextFeatures(element: PsiElement, features: Map<String, MLFeatureValue>) {
      element.putUserData(CONTEXT_FEATURES_KEY, ContextFeatures(Collections.unmodifiableMap(features)))
    }

    fun clear(element: PsiElement) {
      element.putUserData(CONTEXT_FEATURES_KEY, null)
    }
  }

  fun binaryValue(name: String): Boolean? = (featuresSnapshot[name] as? BinaryValue)?.value

  fun floatValue(name: String): Double? = (featuresSnapshot[name] as? FloatValue)?.value

  fun categoricalValue(name: String): String? = (featuresSnapshot[name] as? CategoricalValue<*>)?.toString()
}


// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml

import com.intellij.codeInsight.completion.ml.ContextFeatures
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.openapi.util.UserDataHolderBase

class ContextFeaturesStorage(private val featuresSnapshot: Map<String, MLFeatureValue>) : ContextFeatures, UserDataHolderBase() {
  companion object {
    val EMPTY = ContextFeaturesStorage(emptyMap())
  }

  override fun binaryValue(name: String): Boolean? = findValue<MLFeatureValue.BinaryValue>(name)?.value

  override fun floatValue(name: String): Double? = findValue<MLFeatureValue.FloatValue>(name)?.value

  override fun categoricalValue(name: String): String? = findValue<MLFeatureValue.CategoricalValue>(name)?.value

  override fun classNameValue(name: String): String? = findValue<MLFeatureValue.ClassNameValue>(name)
    ?.let { MLFeaturesUtil.valueAsString(it) }

  override fun asMap(): Map<String, String> = featuresSnapshot.mapValues { MLFeaturesUtil.valueAsString(it.value) }

  private inline fun <reified T : MLFeatureValue> findValue(name: String): T? {
    return featuresSnapshot[name] as? T
  }
}


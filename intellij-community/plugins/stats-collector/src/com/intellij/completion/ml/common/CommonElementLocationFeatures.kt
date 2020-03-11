// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.ml.ContextFeatures
import com.intellij.codeInsight.completion.ml.ElementFeatureProvider
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator

class CommonElementLocationFeatures : ElementFeatureProvider {
  override fun getName(): String = "common"

  override fun calculateFeatures(
    element: LookupElement,
    location: CompletionLocation,
    contextFeatures: ContextFeatures
  ): MutableMap<String, MLFeatureValue> {

    val result = mutableMapOf<String, MLFeatureValue>()

    val completionElement = delegate(element).psiElement

    // ruby blocks tree access in tests - org.jetbrains.plugins.ruby.ruby.testCases.RubyCodeInsightTestFixture.complete
    if (completionElement?.language?.isKindOf("ruby") != true) {
      val linesDiff = LocationFeaturesUtil.linesDiff(location.completionParameters, completionElement)
      if (linesDiff != null) {
        result["lines_diff"] = MLFeatureValue.float(linesDiff)
      }
    }

    completionElement?.let {
      result["item_class"] = MLFeatureValue.className(it::class.java)
    }

    return result
  }

  private tailrec fun delegate(lookupElement: LookupElement): LookupElement = when (lookupElement) {
    is LookupElementDecorator<*> -> delegate(lookupElement.delegate)
    else -> lookupElement
  }
}
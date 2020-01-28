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
    return mutableMapOf(
      "lines_diff" to MLFeatureValue.float(LocationFeaturesUtil.linesDiff(location.completionParameters, delegate(element).psiElement))
    )
  }

  private tailrec fun delegate(lookupElement: LookupElement): LookupElement = when (lookupElement) {
    is LookupElementDecorator<*> -> delegate(lookupElement.delegate)
    else -> lookupElement
  }
}
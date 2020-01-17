// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.ml.ContextFeatures
import com.intellij.codeInsight.completion.ml.ElementFeatureProvider
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.codeInsight.lookup.LookupElement

class NGramFeatureProvider : ElementFeatureProvider {

  override fun getName(): String {
    return "ngram"
  }

  override fun calculateFeatures(element: LookupElement,
                                 location: CompletionLocation,
                                 contextFeatures: ContextFeatures): Map<String, MLFeatureValue> {
    val scoringFunction = contextFeatures.getUserData(NGram.NGRAM_SCORER_KEY) ?: return emptyMap()
    return mapOf("file" to MLFeatureValue.float(scoringFunction.score(element.lookupString)))
  }

}


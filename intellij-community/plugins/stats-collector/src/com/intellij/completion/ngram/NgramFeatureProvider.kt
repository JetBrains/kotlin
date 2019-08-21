// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.ml.ContextFeatures
import com.intellij.codeInsight.completion.ml.ElementFeatureProvider
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.completion.ngram.Ngram.Companion.NGRAM_PREFIX_KEY
import com.intellij.completion.ngram.NgramFileConfigurator.Companion.getModelRunner

class NgramFeatureProvider : ElementFeatureProvider {

  override fun getName(): String {
    return "ngram"
  }

  override fun calculateFeatures(element: LookupElement,
                                 location: CompletionLocation,
                                 contextFeatures: ContextFeatures): Map<String, MLFeatureValue> {
    val modelRunner = getModelRunner(location.completionParameters.originalFile) ?: return emptyMap()
    val ngramPrefix = contextFeatures.getUserData(NGRAM_PREFIX_KEY) ?: return emptyMap()
    val ngram = Ngram(listOf(*ngramPrefix, element.lookupString), ngramPrefix.size + 1)
    return mapOf("file" to MLFeatureValue.float(modelRunner.getNgramProbability(ngram)))
  }

}


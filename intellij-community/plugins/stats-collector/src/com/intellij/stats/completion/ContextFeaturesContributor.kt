// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.ml.CompletionEnvironment
import com.intellij.codeInsight.completion.ml.ContextFeatureProvider
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.completion.ml.ContextFeaturesStorage
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.stats.storage.factors.MutableLookupStorage
import java.util.concurrent.ConcurrentHashMap

class ContextFeaturesContributor : CompletionContributor() {
  override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
    val lookup = LookupManager.getActiveLookup(parameters.editor) as? LookupImpl
    if (lookup != null) {
      val storage = MutableLookupStorage.get(lookup)
      if (storage != null && !storage.isContextFactorsInitialized()) {
        calculateContextFactors(lookup, parameters, storage)
      }
    }
    super.fillCompletionVariants(parameters, result)
  }


  private fun calculateContextFactors(lookup: LookupImpl, parameters: CompletionParameters, storage: MutableLookupStorage) {
    val file = lookup.psiFile
    if (file != null) {
      val context = MyEnvironment(lookup, parameters)
      val contextFeatures = mutableMapOf<String, MLFeatureValue>()
      for (provider in ContextFeatureProvider.forLanguage(storage.language)) {
        ProgressManager.checkCanceled()
        val providerName = provider.name
        for ((featureName, value) in provider.calculateFeatures(context)) {
          contextFeatures["ml_ctx_${providerName}_$featureName"] = value
        }
      }

      ContextFeaturesStorage.setContextFeatures(file, contextFeatures, context)
      Disposer.register(lookup, Disposable { ContextFeaturesStorage.clear(file) })
      storage.initContextFactors(contextFeatures.mapValuesTo(ConcurrentHashMap()) { it.value.toString() })
    }
  }

  private class MyEnvironment(
    private val lookup: LookupImpl,
    private val parameters: CompletionParameters
  ) : CompletionEnvironment, UserDataHolderBase() {
    override fun getLookup(): Lookup = lookup

    override fun getParameters(): CompletionParameters = parameters
  }
}
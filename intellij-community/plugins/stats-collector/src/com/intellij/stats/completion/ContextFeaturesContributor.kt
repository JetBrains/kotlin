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
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.stats.storage.factors.MutableLookupStorage
import java.util.concurrent.TimeUnit

class ContextFeaturesContributor : CompletionContributor() {
  override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
    val lookup = LookupManager.getActiveLookup(parameters.editor) as? LookupImpl
    if (lookup != null) {
      val storage = MutableLookupStorage.get(lookup)
      if (storage != null && storage.shouldComputeFeatures() && !storage.isContextFactorsInitialized()) {
        calculateContextFactors(lookup, parameters, storage)
      }
    }
    super.fillCompletionVariants(parameters, result)
  }


  private fun calculateContextFactors(lookup: LookupImpl, parameters: CompletionParameters, storage: MutableLookupStorage) {
    val environment = MyEnvironment(lookup, parameters)
    val contextFeatures = mutableMapOf<String, MLFeatureValue>()
    for (provider in ContextFeatureProvider.forLanguage(storage.language)) {
      ProgressManager.checkCanceled()
      val providerName = provider.name
      val start = System.nanoTime()
      val features = provider.calculateFeatures(environment)
      for ((featureName, value) in features) {
        contextFeatures["ml_ctx_${providerName}_$featureName"] = value
      }

      val timeSpent = System.nanoTime() - start
      storage.performanceTracker.contextFeaturesCalculated(providerName, TimeUnit.NANOSECONDS.toMillis(timeSpent))
    }
    storage.initContextFactors(contextFeatures, environment)
  }


  private class MyEnvironment(
    private val lookup: LookupImpl,
    private val parameters: CompletionParameters
  ) : CompletionEnvironment, UserDataHolderBase() {
    override fun getLookup(): Lookup = lookup

    override fun getParameters(): CompletionParameters = parameters
  }
}
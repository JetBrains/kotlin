// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.storage.factors

import com.intellij.codeInsight.completion.ml.ContextFeatures
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.completion.ml.ContextFeaturesStorage
import com.intellij.completion.sorting.RankingModelWrapper
import com.intellij.completion.sorting.RankingSupport
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.reporting.isUnitTestMode
import com.intellij.stats.PerformanceTracker
import com.intellij.stats.completion.idString
import com.intellij.stats.personalization.UserFactorStorage
import com.intellij.stats.personalization.UserFactorsManager
import com.intellij.stats.personalization.session.LookupSessionFactorsStorage
import org.jetbrains.annotations.TestOnly

class MutableLookupStorage(
  override val startedTimestamp: Long,
  override val language: Language,
  override val model: RankingModelWrapper?)
  : LookupStorage {
  private var _userFactors: Map<String, String>? = null
  override val userFactors: Map<String, String>
    get() = _userFactors ?: emptyMap()

  private var contextFeaturesStorage: ContextFeatures? = null
  override val contextFactors: Map<String, String>
    get() = contextFeaturesStorage?.asMap() ?: emptyMap()

  private var mlUsed: Boolean = false

  private var _loggingEnabled: Boolean = false
  override val performanceTracker: PerformanceTracker = PerformanceTracker()

  companion object {
    private val LOG = logger<MutableLookupStorage>()
    private val LOOKUP_STORAGE = Key.create<MutableLookupStorage>("completion.ml.lookup.storage")

    @Volatile
    private var alwaysComputeFeaturesInTests = true

    @TestOnly
    fun setComputeFeaturesAlways(value: Boolean, parentDisposable: Disposable) {
      val valueBefore = alwaysComputeFeaturesInTests
      alwaysComputeFeaturesInTests = value
      Disposer.register(parentDisposable, Disposable {
        alwaysComputeFeaturesInTests = valueBefore
      })
    }

    fun get(lookup: LookupImpl): MutableLookupStorage? {
      return lookup.getUserData(LOOKUP_STORAGE)
    }

    fun initOrGetLookupStorage(lookup: LookupImpl, language: Language): MutableLookupStorage {
      val existed = get(lookup)
      if (existed != null) return existed
      val storage = MutableLookupStorage(System.currentTimeMillis(), language, RankingSupport.getRankingModel(language))
      lookup.putUserData(LOOKUP_STORAGE, storage)
      return storage
    }
  }

  override val sessionFactors: LookupSessionFactorsStorage = LookupSessionFactorsStorage(startedTimestamp)

  private val item2storage: MutableMap<String, MutableElementStorage> = mutableMapOf()

  override fun getItemStorage(id: String): MutableElementStorage = item2storage.computeIfAbsent(id) {
    MutableElementStorage()
  }

  override fun mlUsed(): Boolean = mlUsed

  fun fireReorderedUsingMLScores() {
    mlUsed = true
    performanceTracker.reorderedByML()
  }

  override fun shouldComputeFeatures(): Boolean = model != null || _loggingEnabled || (isUnitTestMode() && alwaysComputeFeaturesInTests)

  fun isContextFactorsInitialized(): Boolean = contextFeaturesStorage != null

  fun fireElementScored(element: LookupElement, factors: MutableMap<String, Any>, mlScore: Double?) {
    getItemStorage(element.idString()).fireElementScored(factors, mlScore)
  }

  fun initUserFactors(project: Project) {
    ApplicationManager.getApplication().assertIsDispatchThread()
    if (_userFactors != null) {
      LOG.error("User factors should be initialized only once")
    }
    else {
      val userFactorValues = mutableMapOf<String, String>()
      val userFactors = UserFactorsManager.getInstance().getAllFactors()
      val applicationStorage: UserFactorStorage = UserFactorStorage.getInstance()
      val projectStorage: UserFactorStorage = UserFactorStorage.getInstance(project)
      for (factor in userFactors) {
        factor.compute(applicationStorage)?.let { userFactorValues["${factor.id}:App"] = it }
        factor.compute(projectStorage)?.let { userFactorValues["${factor.id}:Project"] = it }
      }
      _userFactors = userFactorValues
    }
  }

  override fun contextProvidersResult(): ContextFeatures = contextFeaturesStorage ?: ContextFeaturesStorage.EMPTY

  fun initContextFactors(contextFactors: MutableMap<String, MLFeatureValue>,
                         environment: UserDataHolderBase) {
    if (isContextFactorsInitialized()) {
      LOG.error("Context factors should be initialized only once")
    }
    else {
      val features = ContextFeaturesStorage(contextFactors)
      environment.copyUserDataTo(features)
      contextFeaturesStorage = features
    }
  }

  fun markLoggingEnabled() {
    _loggingEnabled = true
  }
}
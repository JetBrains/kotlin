// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.storage.factors

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.completion.sorting.RankingModelWrapper
import com.intellij.completion.sorting.RankingSupport
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.stats.PerformanceTracker
import com.intellij.stats.completion.idString
import com.intellij.stats.personalization.UserFactorStorage
import com.intellij.stats.personalization.UserFactorsManager
import com.intellij.stats.personalization.session.LookupSessionFactorsStorage

class MutableLookupStorage(
  override val startedTimestamp: Long,
  override val language: Language,
  override val model: RankingModelWrapper)
  : LookupStorage {
  private var _userFactors: Map<String, String>? = null
  override val userFactors: Map<String, String>
    get() = _userFactors ?: emptyMap()

  @Volatile
  private var _contextFactors: Map<String, String>? = null
  override val contextFactors: Map<String, String>
    get() = _contextFactors ?: emptyMap()

  override val performanceTracker: PerformanceTracker = PerformanceTracker()

  companion object {
    private val LOG = logger<MutableLookupStorage>()
    private val LOOKUP_STORAGE = Key.create<MutableLookupStorage>("completion.ml.lookup.storage")

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

  fun isContextFactorsInitialized(): Boolean = _contextFactors != null

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

  fun initContextFactors(contextFactors: Map<String, String>) {
    if (_contextFactors != null) {
      LOG.error("Context factors should be initialized only once")
    }
    else {
      _contextFactors = contextFactors
    }
  }
}
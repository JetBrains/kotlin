// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.completion

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import com.jetbrains.completion.feature.*
import com.jetbrains.completion.feature.impl.CompletionFactors
import com.jetbrains.completion.feature.impl.FeatureInterpreterImpl
import com.jetbrains.completion.feature.impl.FeatureManagerFactory
import com.jetbrains.completion.feature.impl.FeatureReader

class FeatureManagerImpl : FeatureManager, BaseComponent {
  companion object {
    fun getInstance(): FeatureManager = ApplicationManager.getApplication().getComponent(FeatureManager::class.java)
  }

  private lateinit var manager: FeatureManager

  override val binaryFactors: List<BinaryFeature> get() = manager.binaryFactors
  override val doubleFactors: List<DoubleFeature> get() = manager.doubleFactors
  override val categoricalFactors: List<CategoricalFeature> get() = manager.categoricalFactors
  override val completionFactors: CompletionFactors get() = manager.completionFactors
  override val featureOrder: Map<String, Int> get() = manager.featureOrder

  override fun createTransformer(): Transformer {
    return manager.createTransformer()
  }

  override fun isUserFeature(name: String): Boolean = false

  override fun initComponent() {
    manager = FeatureManagerFactory().createFeatureManager(FeatureReader, FeatureInterpreterImpl())
  }

  override fun allFeatures(): List<Feature> = manager.allFeatures()
}
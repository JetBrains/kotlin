// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.parameter

import com.intellij.codeInsight.hints.HintsProviderInfo
import com.intellij.codeInsight.hints.InlayHintsProviderFactory
import com.intellij.codeInsight.hints.ProviderWithSettings
import com.intellij.openapi.project.Project

class NewParameterHintsProviderFactory : InlayHintsProviderFactory {
  override fun getProvidersInfo(project: Project): List<HintsProviderInfo<out Any>> {
    return NewParameterHintsProvider.all().map { HintsProviderInfo(it.second.withSettings(), it.first) }
  }
}

fun <T : Any> NewParameterHintsProvider<T>.withSettings(): ProviderWithSettings<ParameterHintsSettings<T>> {
  val provider = NewParameterHintsInlayProvider(this)
  return ProviderWithSettings(provider, provider.createSettings())
}
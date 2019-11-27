// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings

import com.intellij.codeInsight.hints.InlayHintsProviderExtension
import com.intellij.codeInsight.hints.InlayHintsSettings
import com.intellij.codeInsight.hints.settings.language.SingleLanguageInlayHintsConfigurable
import com.intellij.codeInsight.hints.withSettings
import com.intellij.ide.ui.search.SearchableOptionContributor
import com.intellij.ide.ui.search.SearchableOptionProcessor

class InlayHintsSettingsSearchableContributor : SearchableOptionContributor() {
  override fun processOptions(processor: SearchableOptionProcessor) {
    for (providerInfo in InlayHintsProviderExtension.findProviders()) {
      val provider = providerInfo.provider
      val name = provider.name
      val id = SingleLanguageInlayHintsConfigurable.getId(providerInfo.language)
      processor.addOptions(name, null, null, id, null, false)
      val providerWithSettings = provider.withSettings(providerInfo.language, InlayHintsSettings.instance())
      for (case in providerWithSettings.configurable.cases) {
        processor.addOptions(case.name, null, null, id, null, false)
      }
    }
  }
}
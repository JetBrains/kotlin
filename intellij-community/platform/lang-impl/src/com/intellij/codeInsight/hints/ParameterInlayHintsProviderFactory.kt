// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ParameterInlayHintsProviderFactory : InlayHintsProviderFactory {
  override fun getProvidersInfo(project: Project): List<HintsProviderInfo<NoSettings>> {
    val config = project.service<InlayHintsSettings>()
    return getHintProviders().map { (lang, provider) ->
      HintsProviderInfo(ProxyInlayParameterHintsProvider(provider, lang).withSettings(lang, config), lang)
    }
  }
}

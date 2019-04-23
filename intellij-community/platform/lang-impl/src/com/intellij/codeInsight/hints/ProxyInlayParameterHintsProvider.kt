// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.config.ParameterHintsSettingsConfigurable
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

/**
 * This proxy class is required to preserve compatibility with [InlayParameterHintsProvider],
 * that appeared earlier this more general API
 */
class ProxyInlayParameterHintsProvider(
  private val parameterHintsProvider: InlayParameterHintsProvider,
  private val language: Language
) : InlayHintsProvider<NoSettings> {
  // TODO actually, contract is broken, inside implementation every language uses global settings, so preview is not working now
  // TODO we should create some kind of scopes for Option to make changes visible in preview

  private val options = parameterHintsProvider.supportedOptions


  override fun getCollectorFor(file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink): InlayHintsCollector<NoSettings>? {
    if (editor.isOneLineMode) return null
    val currentStamp = ParameterHintsPassFactory.getCurrentModificationStamp(file)
    val savedStamp = editor.getUserData<Long>(ParameterHintsPassFactory.PSI_MODIFICATION_STAMP)
    if (savedStamp != null && savedStamp == currentStamp) return null
    val language = file.language
    // TODO return correct!
    return null
//    return ParameterHintsPass(file, MethodInfoBlacklistFilter.forLanguage(language), false, ourKey)
  }

  override val key: SettingsKey<NoSettings> = ourKey

  override fun createConfigurable(settings: NoSettings): ImmediateConfigurable = ParameterHintsSettingsConfigurable(
    options, language, parameterHintsProvider.isBlackListSupported)

  override fun createSettings() = NoSettings()

  override val name: String = "Parameter hints"

  override val previewText: String? = parameterHintsProvider.settingsPreview

  companion object {
    @JvmStatic
    val ourKey = SettingsKey<NoSettings>("parameter.hints")

    @JvmStatic
    fun isEnabledFor(language: Language, settings: InlayHintsSettings): Boolean {
      return settings.hintsEnabled(ourKey, language)
    }
  }
}
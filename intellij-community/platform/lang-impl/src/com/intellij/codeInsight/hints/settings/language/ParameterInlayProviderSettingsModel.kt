// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings.language

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.settings.InlayProviderSettingsModel
import com.intellij.codeInsight.hints.settings.ParameterHintsSettingsPanel
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class ParameterInlayProviderSettingsModel(
  val provider: InlayParameterHintsProvider,
  val language: Language
) : InlayProviderSettingsModel(
  isParameterHintsEnabledForLanguage(language), "parameter.hints.old") {
  override val mainCheckBoxLabel: String
    get() = provider.mainCheckboxText
  override val name: String
    get() = "Parameter hints"

  override val previewText: String?
    get() = null
  override val component by lazy {
    ParameterHintsSettingsPanel(
      language = language,
      blackListSupported = provider.isBlackListSupported
    )
  }

  private val optionStates = provider.supportedOptions.map { OptionState(it) }

  override val cases: List<ImmediateConfigurable.Case> = provider.supportedOptions.mapIndexed { index, option ->
    val state = optionStates[index]
    ImmediateConfigurable.Case(option.name,
                               id = option.id,
                               loadFromSettings = { state.state },
                               onUserChanged = { state.state = it },
                               extendedDescription = option.extendedDescription
    )
  }

  override fun collectAndApply(editor: Editor, file: PsiFile) = throw UnsupportedOperationException()


  override fun toString(): String = name

  override fun apply() {
    setShowParameterHintsForLanguage(isEnabled, language)
    for (state in optionStates) {
      state.apply()
    }
  }

  override fun isModified(): Boolean {
    if (isEnabled != isParameterHintsEnabledForLanguage(language)) return true
    return optionStates.any { it.isModified() }
  }

  override fun reset() {
    isEnabled = isParameterHintsEnabledForLanguage(language)
    for (state in optionStates) {
      state.reset()
    }
  }

  private data class OptionState(val option: Option, var state: Boolean = option.get()) {
    fun isModified(): Boolean = state != option.get()

    fun reset() {
      state = option.get()
    }

    fun apply() {
      option.set(state)
    }
  }
}
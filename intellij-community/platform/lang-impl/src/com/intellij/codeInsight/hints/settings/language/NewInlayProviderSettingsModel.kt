// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings.language

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.settings.InlayProviderSettingsModel
import com.intellij.configurationStore.deserializeInto
import com.intellij.configurationStore.serialize
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class NewInlayProviderSettingsModel<T : Any>(
  private val providerWithSettings: ProviderWithSettings<T>,
  private val config: InlayHintsSettings
) : InlayProviderSettingsModel(
  isEnabled = config.hintsEnabled(providerWithSettings.provider.key, providerWithSettings.language),
  id = providerWithSettings.provider.key.id
) {
  override val name: String
    get() = providerWithSettings.provider.name
  override val mainCheckBoxLabel: String
    get() = providerWithSettings.configurable.mainCheckboxText

  override val component by lazy {
    providerWithSettings.configurable.createComponent(onChangeListener!!)
  }
  override fun collectAndApply(editor: Editor, file: PsiFile) {
    providerWithSettings.getCollectorWrapperFor(file, editor, providerWithSettings.language)?.collectTraversingAndApply(editor, file)
  }

  override val cases: List<ImmediateConfigurable.Case>
    get() = providerWithSettings.configurable.cases

  override val previewText: String?
    get() = providerWithSettings.provider.previewText


  override fun apply() {
    val copy = providerWithSettings.withSettingsCopy()
    config.storeSettings(copy.provider.key, copy.language, copy.settings)
    config.changeHintTypeStatus(copy.provider.key, copy.language, isEnabled)
  }

  override fun isModified(): Boolean {
    if (isEnabled != config.hintsEnabled(providerWithSettings.provider.key, providerWithSettings.language)) return true
    val inSettings = providerWithSettings.settings
    val stored = providerWithSettings.provider.getActualSettings(config, providerWithSettings.language)
    return inSettings != stored
  }

  override fun toString(): String = name

  override fun reset() {
    // Workaround for deep copy
    val obj = providerWithSettings.provider.getActualSettings(config, providerWithSettings.language)
    serialize(obj)?.deserializeInto(providerWithSettings.settings)
    providerWithSettings.configurable.reset()
    isEnabled = config.hintsEnabled(providerWithSettings.provider.key, providerWithSettings.language)
  }
}
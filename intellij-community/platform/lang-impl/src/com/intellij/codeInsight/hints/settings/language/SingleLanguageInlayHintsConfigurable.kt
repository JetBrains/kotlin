// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings.language

import com.intellij.codeInsight.hints.InlayHintsPassFactory
import com.intellij.codeInsight.hints.ParameterHintsPassFactory
import com.intellij.codeInsight.hints.settings.InlayProviderSettingsModel
import com.intellij.codeInsight.hints.settings.InlaySettingsProvider
import com.intellij.lang.Language
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent


class SingleLanguageInlayHintsConfigurable(project: Project, val language: Language) : Configurable, SearchableConfigurable {
  private val panel by lazy {
    SingleLanguageInlayHintsSettingsPanel(getInlayProviderSettingsModels(project, language), language, project)
  }

  override fun isModified(): Boolean {
    return panel.isModified()
  }

  override fun getDisplayName(): String {
    return language.displayName
  }

  override fun createComponent(): JComponent? {
    return panel
  }

  override fun apply() {
    panel.apply()
    ParameterHintsPassFactory.forceHintsUpdateOnNextPass()
    InlayHintsPassFactory.forceHintsUpdateOnNextPass()
  }

  override fun getId(): String {
    return getId(language)
  }

  override fun getHelpTopic(): String {
    return getHelpTopic(language)
  }

  internal fun getModels(): Array<InlayProviderSettingsModel> {
    return panel.getModels()
  }

  internal fun setCurrentModel(model: InlayProviderSettingsModel) {
    panel.setCurrentModel(model)
  }

  companion object {
    fun getInlayProviderSettingsModels(project: Project, language: Language) : Array<InlayProviderSettingsModel> {
      val models = InlaySettingsProvider.EP.getExtensions().flatMap { it.createModels(project, language) }
      if (models.isEmpty()) throw IllegalStateException("Language panel must have at least one config model")
      return models.toTypedArray()
    }

    @JvmStatic
    fun getId(language: Language): String {
      return "inlay.hints." + language.id
    }

    @JvmStatic
    fun getHelpTopic(language: Language): String {
      return "settings.inlayhints." + language.id
    }
  }

  override fun reset() {
    panel.reset()
  }
}




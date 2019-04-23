// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.parameter

import com.intellij.codeInsight.hints.HintWidthAdjustment
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement

interface NewParameterHintsProvider<T: Any> {
  fun getCollector(element: PsiElement, settings: T, editor: Editor) : ParameterHintsCollector<T>

  /**
   * Some unique key, that will be used for settings persistance
   */
  val settingsKey: SettingsKey<ParameterHintsSettings<T>>

  val preview: String

  /**
   * Creates new instance of settings object
   */
  fun createSettings(): T

  fun createConfigurable(settings: T) : ImmediateConfigurable

  val blackList: BlackListConfiguration?


  companion object {
    private const val NAME = "com.intellij.codeInsight.parameterHintsProvider"

    @JvmStatic
    val EP = LanguageExtension<NewParameterHintsProvider<out Any>>(NAME)

    private val EP_NAME = ExtensionPointName<LanguageExtensionPoint<NewParameterHintsProvider<out Any>>>(NAME)

    @JvmStatic
    fun all(): List<Pair<Language, NewParameterHintsProvider<out Any>>> {
      return EP_NAME.extensions.mapNotNull { Language.findLanguageByID(it.language) }
        .map { lang ->  lang to EP.forLanguage(lang) }
    }
  }
}

interface ParameterHintsCollector<T: Any> {
  /**
   * Hints for params to be shown, hints offsets should be located within elements text range
   */
  fun getParameterHints(element: PsiElement, settings: T, sink: ParameterHintsSink)
}

abstract class FactoryHintsCollector<T : Any>(editor: Editor) : ParameterHintsCollector<T> {
  val factory = PresentationFactory(editor as EditorImpl)
}

interface ParameterHintsSink {
  fun addHint(info: ParameterHintInfo)
}

interface BlackListConfiguration {
  val defaultBlackList: Set<String>

  val blackListDependencyLanguage: Language?

  val blackListExplanationHtml: String?
}

class ParameterHintInfo(
  val presentation: InlayPresentation,
  val offset: Int,
  val isShowOnlyIfExistedBefore: Boolean,
  val relatesToPrecedingText: Boolean,
  val blackListInfo: BlackListInfo?,
  val widthAdjustment: HintWidthAdjustment?
)

class BlackListInfo(
  val fullyQualifiedName: String,
  val parameterNames: List<String>
)
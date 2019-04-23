// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.parameter

import com.intellij.codeInsight.hints.*
import com.intellij.diff.util.DiffUtil
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class NewParameterHintsInlayProvider<T: Any>(val provider: NewParameterHintsProvider<T>) : InlayHintsProvider<ParameterHintsSettings<T>> {
  override val name: String
    get() = "Parameter hints"

  override fun getCollectorFor(file: PsiFile, editor: Editor, settings: ParameterHintsSettings<T>, sink: InlayHintsSink): InlayHintsCollector<ParameterHintsSettings<T>>? {
    if (DiffUtil.isDiffEditor(editor)) return null
    val filter = ParameterBlackListFilter(settings.blackList)
    val parameterHintsSink = object : ParameterHintsSink {
      override fun addHint(info: ParameterHintInfo) {
        val offset = info.offset
        if (!canShowHintsAtOffset(offset, editor.document, file)) return
        val blackListInfo = info.blackListInfo
        if (blackListInfo != null && !filter.shouldShowHint(blackListInfo)) return
        sink.addInlay(offset, info.presentation) // TODO use other info here!!!
      }
    }
    val collector = provider.getCollector(file, settings.providerSettings, editor)
    return object : InlayHintsCollector<ParameterHintsSettings<T>> {
      override val key: SettingsKey<ParameterHintsSettings<T>>
        get() = this@NewParameterHintsInlayProvider.key

      override fun collect(element: PsiElement,
                           editor: Editor,
                           settings: ParameterHintsSettings<T>,
                           isEnabled: Boolean,
                           sink: InlayHintsSink) {
        collector.getParameterHints(element, settings.providerSettings, parameterHintsSink)
      }
    }
  }

  /**
   * Adding hints on the borders of root element (at startOffset or endOffset)
   * is allowed only in the case when root element is a document
   *
   * @return true if a given offset can be used for hint rendering
   */
  private fun canShowHintsAtOffset(offset: Int, document: Document, rootElement: PsiElement): Boolean {
    val rootRange = rootElement.textRange
    if (!rootRange.containsOffset(offset)) return false
    return if (offset > rootRange.startOffset && offset < rootRange.endOffset) true else document.textLength == rootRange.length
  }

  override fun createSettings(): ParameterHintsSettings<T> {
    // TODO merge with base lists?
    return ParameterHintsSettings(provider.blackList?.defaultBlackList ?: emptySet(), provider.createSettings())
  }

  override val key: SettingsKey<ParameterHintsSettings<T>>
    get() = provider.settingsKey
  override val previewText: String?
    get() = provider.preview

  override fun createConfigurable(settings: ParameterHintsSettings<T>): ImmediateConfigurable = provider.createConfigurable(settings.providerSettings)
}

class ParameterHintsSettings<T>(val blackList: Set<String>, val providerSettings: T)


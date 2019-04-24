// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationListener
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayModel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.util.DocumentUtil
import gnu.trove.TIntObjectHashMap
import java.awt.Dimension
import java.awt.Rectangle

interface InlayHintsSink {
  /**
   * Adds inlay to underlying editor.
   * Note, that only one presentation with the given key may be at the same offset.
   */
  fun addInlay(offset: Int, presentation: InlayPresentation) // TODO do I need relates to preceding text bits? What is it for?
}

class InlayHintsSinkImpl<T>(val key: SettingsKey<T>) : InlayHintsSink {
  private val hints = TIntObjectHashMap<InlayPresentation>()

  override fun addInlay(offset: Int, presentation: InlayPresentation) {
    hints.put(offset, presentation)
  }

  fun applyToEditor(element: PsiElement, editor: Editor, existingInlays: List<Inlay<EditorCustomElementRenderer>>) {

    val inlayModel = editor.inlayModel
    val isBulkChange = existingInlays.size + hints.size() > BulkChangeThreshold
    DocumentUtil.executeInBulk(editor.document, isBulkChange) {
      updateOrDeleteExistingHints(existingInlays)
      createNewHints(inlayModel)
    }
    hints.clear()
  }

  private fun createNewHints(inlayModel: InlayModel) {
    hints.forEachEntry { offset, presentation ->
      // TODO how should I support vertical hints??
      // TODO probably sealed class to represent hints is required. Later, we will add it to the model and setup listeners
      val inlay = inlayModel.addInlineElement(offset, PresentationRenderer(presentation)) ?: return@forEachEntry true
      inlay.putUserData(INLAY_KEY, key)
      presentation.addListener(InlayListener(inlay))
      true
    }
  }

  class InlayListener(private val inlay: Inlay<PresentationRenderer>) : PresentationListener {
    // TODO be more accurate during invalidation (requires changes in Inlay)
    override fun contentChanged(area: Rectangle) = inlay.repaint()

    override fun sizeChanged(previous: Dimension, current: Dimension) = inlay.updateSize()
  }

  private fun updateOrDeleteExistingHints(existingInlays: List<Inlay<EditorCustomElementRenderer>>) {
    for (inlay in existingInlays) {
      val inlayKey = inlay.getUserData(INLAY_KEY) as SettingsKey<*>?
      if (inlayKey != key) continue
      val offset = inlay.offset
      val newPresentation = hints[offset]
      if (newPresentation == null) {
        Disposer.dispose(inlay)
      }
      else {
        val renderer = inlay.renderer as PresentationRenderer
        val previousPresentation = renderer.presentation
        @Suppress("UNCHECKED_CAST")
        newPresentation.addListener(InlayListener(inlay as Inlay<PresentationRenderer>))
        if (newPresentation.updateState(previousPresentation)) {
          newPresentation.fireUpdateEvent(previousPresentation.dimension())
        }
        renderer.presentation = newPresentation
        hints.remove(offset)
      }
    }
  }

  companion object {
    private val INLAY_KEY: Key<Any?> = Key.create("INLAY_KEY")
    private const val BulkChangeThreshold = 1000
  }

}
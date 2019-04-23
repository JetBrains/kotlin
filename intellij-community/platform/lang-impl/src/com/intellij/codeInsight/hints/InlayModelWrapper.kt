// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.PresentationListener
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayModel
import java.awt.Dimension
import java.awt.Rectangle

/**
 * Wrapper around InlayModel used to setup listeners for presentation renderer.
 */
class InlayModelWrapper(private val model: InlayModel) : InlayModel by model {
  override fun <T : EditorCustomElementRenderer> addInlineElement(offset: Int, relatesToPreceedingText: Boolean, renderer: T) : Inlay<T>? {
    val inlay = model.addInlineElement(offset, relatesToPreceedingText, renderer) ?: return null
    if (renderer is PresentationRenderer) {
      val presentation = renderer.presentation
      presentation.addListener(object: PresentationListener {
        // TODO be more accurate during invalidation
        override fun contentChanged(area: Rectangle) = inlay.repaint()

        override fun sizeChanged(previous: Dimension, current: Dimension) = inlay.updateSize()
      })
    }
    return inlay
  }
}
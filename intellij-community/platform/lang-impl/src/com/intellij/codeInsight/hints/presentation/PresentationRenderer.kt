// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hints.LinearOrderInlayRenderer
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle

class PresentationRenderer(val presentation: InlayPresentation) : EditorCustomElementRenderer, InputHandler by presentation {
  override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
    g as Graphics2D
    g.withTranslated(targetRegion.x, targetRegion.y) {
      presentation.paint(g, LinearOrderInlayRenderer.effectsIn(textAttributes))
    }
  }


  override fun calcWidthInPixels(inlay: Inlay<*>): Int {
    return presentation.width
  }

  // this should not be shown anywhere
  override fun getContextMenuGroupId(inlay: Inlay<*>): String {
    return "DummyActionGroup"
  }

  override fun toString(): String {
    return presentation.toString()
  }
}
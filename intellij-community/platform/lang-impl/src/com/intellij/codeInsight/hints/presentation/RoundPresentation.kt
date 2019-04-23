// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics2D
import java.awt.geom.RoundRectangle2D

/**
 * Makes corners round. Should be used with [InsetPresentation]
 */
class RoundPresentation(presentation: InlayPresentation, val arcWidth: Int, val arcHeight: Int) : StaticDelegatePresentation(presentation) {
  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    val savedClip = g.clip
    g.clip = RoundRectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble(), arcWidth.toDouble(), arcHeight.toDouble())
    try {
      presentation.paint(g, attributes)
    } finally {
      g.clip = savedClip
    }
  }
}
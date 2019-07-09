// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.util.ui.GraphicsUtil
import java.awt.Color
import java.awt.Graphics2D

class RoundWithBackgroundPresentation(
  presentation: InlayPresentation,
  val arcWidth: Int,
  val arcHeight: Int,
  val color: Color? = null,
  val backgroundAlpha : Float = 0.55f
) : StaticDelegatePresentation(presentation) {
  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    val backgroundColor = color ?: attributes.backgroundColor
    if (backgroundColor != null) {
      val alpha = backgroundAlpha
      val config = GraphicsUtil.setupAAPainting(g)
      GraphicsUtil.paintWithAlpha(g, alpha)
      g.color = backgroundColor
      g.fillRoundRect(0, 0, width, height, arcWidth, arcHeight)
      config.restore()
    }
    presentation.paint(g, attributes)
  }
}
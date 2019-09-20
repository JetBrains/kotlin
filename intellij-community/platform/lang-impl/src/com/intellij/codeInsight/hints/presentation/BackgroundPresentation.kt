// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Graphics2D

/**
 * Adds background color.
 */
class BackgroundPresentation(
  presentation: InlayPresentation,
  var color: Color? = null
) : StaticDelegatePresentation(presentation) {
  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    val backgroundColor = color ?: attributes.backgroundColor
    val oldColor = g.color
    if (backgroundColor != null) {
      g.color = backgroundColor
      g.fillRect(0, 0, width, height)
    }
    try {
      presentation.paint(g, attributes)
    } finally {
      g.color = oldColor
    }
  }
}
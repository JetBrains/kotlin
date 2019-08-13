// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.ide.ui.AntialiasingType
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints

/**
 * Draws text.
 */
class TextInlayPresentation(
  override var width: Int,
  override var height: Int,
  var text: String,
  private var yBaseline: Int,
  val fontProvider: (EditorFontType) -> Font
) : BasePresentation() {

  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    val savedHint = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, AntialiasingType.getKeyForCurrentScope(false))
    try {
      val foreground = attributes.foregroundColor
      if (foreground != null) {
        val fontType = when (attributes.fontType) {
          Font.BOLD -> EditorFontType.BOLD
          Font.ITALIC -> EditorFontType.ITALIC
          else -> EditorFontType.PLAIN
        }
        g.font = fontProvider(fontType)
        g.color = foreground
        g.drawString(text, 0, yBaseline)
      }
    } finally {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedHint)
    }
  }

  override fun toString(): String = text
}
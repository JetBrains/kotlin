// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.ide.ui.AntialiasingType
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.paint.EffectPainter
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
  var lineHeight: Int,
  var ascent: Int,
  var descent: Int,
  val fontProvider: (EditorFontType) -> Font
) : BasePresentation() {

  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    val savedHint = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING)
    try {
      val foreground = attributes.foregroundColor
      if (foreground != null) {
        val fontType = when (attributes.fontType) {
          Font.BOLD -> EditorFontType.BOLD
          Font.ITALIC -> EditorFontType.ITALIC
          else -> EditorFontType.PLAIN
        }
        val font = fontProvider(fontType)
        g.font = font
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, AntialiasingType.getKeyForCurrentScope(false))
        g.color = foreground
        g.drawString(text, 0, yBaseline)
        val effectColor = attributes.effectColor
        if (effectColor != null) {
          g.color = effectColor
          when (attributes.effectType) {
            EffectType.LINE_UNDERSCORE -> EffectPainter.LINE_UNDERSCORE.paint(g, 0, ascent, width, descent, font)
            EffectType.BOLD_LINE_UNDERSCORE -> EffectPainter.BOLD_LINE_UNDERSCORE.paint(g, 0, ascent, width, descent, font)
            EffectType.STRIKEOUT -> EffectPainter.STRIKE_THROUGH.paint(g, 0, ascent, width, height, font)
            EffectType.WAVE_UNDERSCORE -> EffectPainter.WAVE_UNDERSCORE.paint(g, 0, ascent, width, descent, font)
            EffectType.BOLD_DOTTED_LINE -> EffectPainter.BOLD_DOTTED_UNDERSCORE.paint(g, 0, ascent, width, descent, font)
            else -> {}
          }
        }
      }
    }
    finally {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedHint)
    }
  }

  override fun toString(): String = text
}
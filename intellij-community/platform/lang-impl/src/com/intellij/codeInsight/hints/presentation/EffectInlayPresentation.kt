// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.paint.EffectPainter
import java.awt.Font
import java.awt.Graphics2D

/**
 * Adds effects to the underlying text
 */
class EffectInlayPresentation(
  presentation: InlayPresentation,
  var font: Font,
  var lineHeight: Int,
  var ascent: Int,
  var descent: Int
) : StaticDelegatePresentation(presentation) {
  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    presentation.paint(g, attributes)
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
// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.ide.ui.AntialiasingType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.CalledInAwt
import java.awt.Font
import java.awt.FontMetrics
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import kotlin.math.ceil
import kotlin.math.max

internal class InlayTextMetricsStorage(val editor: EditorImpl) {
  private var smallTextMetrics : InlayTextMetrics? = null
  private var normalTextMetrics : InlayTextMetrics? = null

  val smallTextSize: Int
    @CalledInAwt
    get() = max(1, editor.colorsScheme.editorFontSize - 1)


  val normalTextSize: Int
    @CalledInAwt
    get() = editor.colorsScheme.editorFontSize

  @CalledInAwt
  fun getFontMetrics(small: Boolean): InlayTextMetrics {
    var metrics: InlayTextMetrics?
    if (small) {
      metrics = smallTextMetrics
      val fontSize = smallTextSize
      if (metrics == null || !metrics.isActual(smallTextSize)) {
        metrics = InlayTextMetrics.create(editor, fontSize)
        smallTextMetrics = metrics
      }
    } else {
      metrics = normalTextMetrics
      val fontSize = normalTextSize
      if (metrics == null || !metrics.isActual(normalTextSize)) {
        metrics = InlayTextMetrics.create(editor, fontSize)
        normalTextMetrics = metrics
      }
    }
    return metrics
  }
}

internal class InlayTextMetrics(
  private val editor: EditorImpl,
  val fontHeight: Int,
  val fontBaseline: Int,
  private val fontMetrics: FontMetrics
) {
  companion object {
    fun create(editor: EditorImpl, size: Int) : InlayTextMetrics {
      val familyName = UIUtil.getLabelFont().family
      val font = UIUtil.getFontWithFallback(familyName, Font.PLAIN, size)
      val context = getCurrentContext(editor)
      val metrics = FontInfo.getFontMetrics(font, context)
      // We assume this will be a better approximation to a real line height for a given font
      val fontHeight = ceil(font.createGlyphVector(context, "Albpq@").visualBounds.height).toInt()
      val fontBaseline = ceil(font.createGlyphVector(context, "Alb").visualBounds.height).toInt()
      return InlayTextMetrics(editor, fontHeight, fontBaseline, metrics)
    }

    private fun getCurrentContext(editor: Editor): FontRenderContext {
      val editorContext = FontInfo.getFontRenderContext(editor.contentComponent)
      return FontRenderContext(editorContext.transform,
                               AntialiasingType.getKeyForCurrentScope(false),
                               if (editor is EditorImpl)
                                 editor.myFractionalMetricsHintValue
                               else
                                 RenderingHints.VALUE_FRACTIONALMETRICS_OFF)
    }
  }

  val font: Font
    get() = fontMetrics.font

  // Editor metrics:
  val ascent: Int
    get() = editor.ascent
  val descent: Int
    get() = editor.descent

  fun isActual(size: Int) : Boolean {
    if (size != font.size) return false
    return getCurrentContext(editor).equals(fontMetrics.fontRenderContext)
  }

  /**
   * Offset from the top edge of drawing rectangle to rectangle with text.
   */
  fun offsetFromTop(): Int = (editor.lineHeight - fontHeight) / 2

  fun getStringWidth(text: String): Int {
    return fontMetrics.stringWidth(text)
  }
}
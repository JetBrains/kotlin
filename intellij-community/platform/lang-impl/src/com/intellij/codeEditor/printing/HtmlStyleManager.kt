// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeEditor.printing

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.intellij.ui.ColorUtil
import com.intellij.ui.Gray
import com.intellij.util.BitUtil
import gnu.trove.THashMap
import java.awt.Color
import java.awt.Font
import java.io.IOException
import java.io.Writer

class HtmlStyleManager(val isInline: Boolean) {
  private val styleMap = THashMap<TextAttributes, String>()
  private val separatorStyleMap = THashMap<Color, String>()

  private val buffer = StringBuilder()

  private val scheme = EditorColorsManager.getInstance().globalScheme

  fun ensureStyles(hIterator: HighlighterIterator, methodSeparators: List<LineMarkerInfo<PsiElement>>) {
    while (!hIterator.atEnd()) {
      val textAttributes = hIterator.textAttributes
      if (!styleMap.containsKey(textAttributes)) {
        val styleName = "s" + styleMap.size
        styleMap.put(textAttributes, styleName)
        buffer.append(".$styleName { ")
        writeTextAttributes(buffer, textAttributes)
        buffer.append("}\n")
      }
      hIterator.advance()
    }

    for (separator in methodSeparators) {
      val color = separator.separatorColor
      if (color != null && !separatorStyleMap.containsKey(color)) {
        val styleName = "ls${separatorStyleMap.size}"
        separatorStyleMap.put(color, styleName)
        val htmlColor = colorToHtml(color)
        buffer.append(".$styleName { height: 1px; border-width: 0; color: $htmlColor; background-color:$htmlColor}\n")
      }
    }
  }

  private fun writeTextAttributes(buffer: Appendable, attributes: TextAttributes) {
    val foreColor = attributes.foregroundColor ?: scheme.defaultForeground
    buffer.append("color: ${colorToHtml(foreColor)};")
    if (BitUtil.isSet(attributes.fontType, Font.BOLD)) {
      buffer.append(" font-weight: bold;")
    }
    if (BitUtil.isSet(attributes.fontType, Font.ITALIC)) {
      buffer.append(" font-style: italic;")
    }
  }

  @Throws(IOException::class)
  fun writeStyleTag(writer: Writer, isUseLineNumberStyle: Boolean) {
    writer.write("<style type=\"text/css\">\n")

    if (isUseLineNumberStyle) {
      val scheme = EditorColorsManager.getInstance().globalScheme
      val lineNumbers = scheme.getColor(EditorColors.LINE_NUMBERS_COLOR)
      buffer.append(String.format(".ln { color: #%s; font-weight: normal; font-style: normal; }\n", ColorUtil.toHex(lineNumbers ?: Gray.x00)))
    }
    writer.append(buffer)
    writer.write("</style>\n")
  }

  fun isDefaultAttributes(attributes: TextAttributes): Boolean {
    return (attributes.foregroundColor ?: scheme.defaultForeground).equals(Color.BLACK) && attributes.fontType == 0
  }

  fun writeTextStyle(writer: Writer, attributes: TextAttributes) {
    writer.write("<span ")
    if (isInline) {
      writer.write("style=\"")
      writeTextAttributes(writer, attributes)
      writer.write("\">")
    }
    else {
      writer.write("class=\"")
      writer.write(styleMap.get(attributes)!!)
      writer.write("\">")
    }
  }

  fun getSeparatorClassName(color: Color): String {
    return separatorStyleMap.get(color)!!
  }
}

internal fun colorToHtml(color: Color) = "#${ColorUtil.toHex(color)}"
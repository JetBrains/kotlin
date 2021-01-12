// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics2D

internal class TextPlaceholderPresentation(
  val length: Int,
  val textMetricsStorage: InlayTextMetricsStorage,
  val small: Boolean
) : BasePresentation() {
  override val width: Int
    get() = EditorUtil.getPlainSpaceWidth(textMetricsStorage.editor) * length
  override val height: Int
    get() = getMetrics().fontHeight

  private fun getMetrics() = textMetricsStorage.getFontMetrics(small)

  override fun paint(g: Graphics2D, attributes: TextAttributes) {}

  override fun toString(): String {
    return " ".repeat(length)
  }
}
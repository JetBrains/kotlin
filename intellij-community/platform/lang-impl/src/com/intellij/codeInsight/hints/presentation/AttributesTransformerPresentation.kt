// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics2D

/**
 * Applies attributes to text in the presentation.
 * Requires to know underlying presentation - attributes of innermost AttributesTransformerPresentation applied.
 */
class AttributesTransformerPresentation(
  presentation: InlayPresentation,
  val transformer: (TextAttributes) -> TextAttributes
) : StaticDelegatePresentation(presentation) {
  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    super.paint(g, transformer(attributes))
  }
}
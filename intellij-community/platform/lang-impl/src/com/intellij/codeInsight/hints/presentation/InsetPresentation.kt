// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics2D
import java.awt.Point
import java.awt.event.MouseEvent

/**
 * Presentation that wraps existing one into rectangle with given insets
 * All mouse events that are outside of inner rectangle are not passed to underlying presentation.
 */
class InsetPresentation(
  presentation: InlayPresentation,
  val left: Int = 0,
  val right: Int = 0,
  val top: Int = 0,
  val down: Int = 0
) : StaticDelegatePresentation(presentation) {
  private var presentationUnderCursor: InlayPresentation? = null

  override val width: Int
    get() = presentation.width + left + right
  override val height: Int
    get() = presentation.height + top + down

  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    g.withTranslated(left, top) {
      presentation.paint(g, attributes)
    }
  }

  private fun handleMouse(e: MouseEvent, original: Point, action: (InlayPresentation, Point) -> Unit) {
    val x = original.x
    val y = original.y
    if (x < left || x >= left + presentation.width || y < top || y >= top + presentation.height) {
      if (presentationUnderCursor != null) {
        presentationUnderCursor?.mouseExited()
        presentationUnderCursor = null
        return
      }
    }
    val translated = original.translateNew(-left, -top)
    action(presentation, translated)
  }

  override fun mouseClicked(event: MouseEvent, translated: Point) {
    handleMouse(event, translated) { presentation, point  ->
      presentation.mouseClicked(event, point)
    }
  }

  override fun mouseMoved(event: MouseEvent, translated: Point) {
    handleMouse(event, translated) { presentation, point  ->
      if (presentation != presentationUnderCursor) {
        presentationUnderCursor?.mouseExited()
        presentationUnderCursor = presentation
      }
      presentation.mouseMoved(event, point)
    }
  }

  override fun mouseExited() {
    try {
      presentationUnderCursor?.mouseExited()
    } finally {
      presentationUnderCursor = null
    }
  }
}
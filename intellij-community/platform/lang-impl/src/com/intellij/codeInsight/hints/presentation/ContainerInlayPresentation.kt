// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.util.ui.GraphicsUtil
import org.jetbrains.annotations.ApiStatus
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.event.MouseEvent

/**
 * Presentation that wraps existing with borders, background and rounded corners if set.
 * @see com.intellij.codeInsight.hints.InlayPresentationFactory.container
 */
@ApiStatus.Experimental
class ContainerInlayPresentation(
  presentation: InlayPresentation,
  private val padding: InlayPresentationFactory.Padding? = null,
  private val roundedCorners: InlayPresentationFactory.RoundedCorners? = null,
  private val background: Color? = null,
  private val backgroundAlpha: Float = 0.55f
) : StaticDelegatePresentation(presentation) {
  private var presentationIsUnderCursor: Boolean = false

  override val width: Int
    get() = leftInset + presentation.width + rightInset
  override val height: Int
    get() = topInset + presentation.height + bottomInset

  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    if (background != null) {

      val preservedBackground = g.background
      g.color = background

      if (roundedCorners != null) {
        val (arcWidth, arcHeight) = roundedCorners
        fillRoundedRectangle(g, height, width, arcWidth, arcHeight, backgroundAlpha)
      } else {
        g.fillRect(0, 0, width, height)
      }
      g.color = preservedBackground
    }
    g.withTranslated(leftInset, topInset) {
      presentation.paint(g, attributes)
    }
  }

  override fun mouseClicked(event: MouseEvent, translated: Point) {
    handleMouse(translated) { point ->
      presentation.mouseClicked(event, point)
    }
  }

  override fun mouseMoved(event: MouseEvent, translated: Point) {
    handleMouse(translated) { point ->
      presentation.mouseClicked(event, point)
    }
  }

  override fun mouseExited() {
    try {
      presentation.mouseExited()
    }
    finally {
      presentationIsUnderCursor = false
    }
  }

  private fun handleMouse(
    original: Point,
    action: (Point) -> Unit
  ) {
    val x = original.x
    val y = original.y
    if (!isInInnerBounds(x, y)) {
      if (presentationIsUnderCursor) {
        presentation.mouseExited()
        presentationIsUnderCursor = false
      }
      return
    }
    val translated = original.translateNew(-leftInset, -topInset)
    action(translated)
  }

  private fun isInInnerBounds(x: Int, y: Int): Boolean {
    return x >= leftInset && x < leftInset + presentation.width && y >= topInset && y < topInset + presentation.height
  }

  private val leftInset: Int
    get() = padding?.left ?: 0
  private val rightInset: Int
    get() = padding?.right ?: 0
  private val topInset: Int
    get() = padding?.top ?: 0
  private val bottomInset: Int
    get() = padding?.bottom ?: 0

  private fun fillRoundedRectangle(g: Graphics2D, height: Int, width: Int, arcWidth: Int, arcHeight: Int, backgroundAlpha: Float) {
    val config = GraphicsUtil.setupAAPainting(g)
    GraphicsUtil.paintWithAlpha(g, backgroundAlpha)
    g.fillRoundRect(0, 0, width, height, arcWidth, arcHeight)
    config.restore()
  }

}
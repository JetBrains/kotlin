// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hints.dimension
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent


/**
 * Intended to store presentations inside block inlay. Aligned to left.
 * Not expected to contain a lot of presentations
 * Must not be empty.
 * @param presentations list of presentations, must not be changed from outside as there is no defensive copying
 */
class VerticalListInlayPresentation(
  val presentations: List<InlayPresentation>
) : BasePresentation() {
  override var width: Int = 0
    private set
  override var height: Int = 0
    private set

  var presentationUnderCursor: InlayPresentation? = null

  init {
    calcDimensions()
    for (presentation in presentations) {
      presentation.addListener(InternalListener(presentation))
    }
  }

  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    var yOffset = 0
    try {
      for (presentation in presentations) {
        presentation.paint(g, attributes)
        yOffset += presentation.height
        g.translate(0, presentation.height)
      }
    }
    finally {
      g.translate(0, -yOffset)
    }
  }

  override fun mouseClicked(event: MouseEvent, translated: Point) {
    handleMouse(translated) { presentation, point ->
      presentation.mouseClicked(event, point)
    }
  }

  override fun mouseMoved(event: MouseEvent, translated: Point) {
    handleMouse(translated) { presentation, point ->
      presentation.mouseMoved(event, point)
    }
  }

  override fun mouseExited() {
    changePresentationUnderCursor(null)
  }

  fun calcDimensions() {
    width = presentations.maxBy { it.width }!!.width
    height = presentations.sumBy { it.height }
  }



  private fun handleMouse(
    original: Point,
    action: (InlayPresentation, Point) -> Unit
  ) {
    val x = original.x
    val y = original.y
    if (x < 0 || x >= width || y < 0 || y >= height) return
    var yOffset = 0
    for (presentation in presentations) {
      val presentationHeight = presentation.height
      if (y < yOffset + presentationHeight && x < presentation.width) {
        changePresentationUnderCursor(presentation)

        val translated = original.translateNew(0, -yOffset)
        action(presentation, translated)
        return
      }
      yOffset += presentationHeight
    }
  }

  private fun changePresentationUnderCursor(presentation: InlayPresentation?) {
    if (presentationUnderCursor != presentation) {
      presentationUnderCursor?.mouseExited()
      presentationUnderCursor = presentation
    }
  }


  override fun toString(): String {
    return presentations.joinToString("\n")
  }

  // TODO area is incorrect, for now rely that all hint's area will be repainted
  private inner class InternalListener(private val currentPresentation: InlayPresentation) : PresentationListener {
    override fun contentChanged(area: Rectangle) {
      area.add(shiftOfCurrent(), 0)
      this@VerticalListInlayPresentation.fireContentChanged(area)
    }

    override fun sizeChanged(previous: Dimension, current: Dimension) {
      val old = dimension()
      calcDimensions()
      val new = dimension()
      this@VerticalListInlayPresentation.fireSizeChanged(old, new)
    }

    private fun shiftOfCurrent(): Int {
      var shift = 0
      for (presentation in presentations) {
        if (presentation === currentPresentation) {
          return shift
        }
        shift += presentation.height
      }
      throw IllegalStateException()
    }
  }
}
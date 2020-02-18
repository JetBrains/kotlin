// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hints.PresentationContainerRenderer
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import java.awt.Point

/**
 * Global mouse listeners that provide events to inlay hints at mouse coordinates.
 */
class InlayEditorMouseListener : EditorMouseListener {
  override fun mouseClicked(e: EditorMouseEvent) {
    if (e.isConsumed) return
    val editor = e.editor
    val event = e.mouseEvent
    if (editor.getMouseEventArea(event) != EditorMouseEventArea.EDITING_AREA) return
    val point = event.point
    val inlay = editor.inlayModel.getElementAt(point, PresentationContainerRenderer::class.java) ?: return
    val bounds = inlay.bounds ?: return
    val inlayPoint = Point(bounds.x, bounds.y)
    val translated = Point(event.x - inlayPoint.x, event.y - inlayPoint.y)
    inlay.renderer.mouseClicked(event, translated)
  }
}

class InlayEditorMouseMotionListener : EditorMouseMotionListener {
  private var activeContainer: PresentationContainerRenderer<*>? = null

  override fun mouseMoved(e: EditorMouseEvent) {
    if (e.isConsumed) return
    val editor = e.editor
    val event = e.mouseEvent
    if (editor.getMouseEventArea(event) != EditorMouseEventArea.EDITING_AREA) {
      activeContainer?.mouseExited()
      activeContainer = null
      return
    }
    val inlay = editor.inlayModel.getElementAt(event.point, PresentationContainerRenderer::class.java)
    val container = inlay?.renderer
    if (activeContainer != container) {
      activeContainer?.mouseExited()
      activeContainer = container
    }
    if (container == null) return
    val bounds = inlay.bounds ?: return
    val inlayPoint = Point(bounds.x, bounds.y)
    val translated = Point(event.x - inlayPoint.x, event.y - inlayPoint.y)
    container.mouseMoved(event, translated)
  }
}
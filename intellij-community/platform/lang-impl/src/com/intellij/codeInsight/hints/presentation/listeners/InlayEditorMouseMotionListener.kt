// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation.listeners

import com.intellij.codeInsight.hints.presentation.InputHandler
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import java.awt.Point

class InlayEditorMouseMotionListener : EditorMouseMotionListener {
  private var activeContainer: InputHandler? = null

  override fun mouseMoved(e: EditorMouseEvent) {
    if (e.isConsumed) return
    val event = e.mouseEvent
    if (e.area != EditorMouseEventArea.EDITING_AREA) {
      activeContainer?.mouseExited()
      activeContainer = null
      return
    }
    val inlay = e.inlay
    val container = inlay?.renderer
    if (activeContainer != container) {
      activeContainer?.mouseExited()
      if (container == null) {
        activeContainer = null
      }
      else if (container is InputHandler) {
        activeContainer = container
      }
    }
    if (container !is InputHandler) return
    val bounds = inlay.bounds ?: return
    val inlayPoint = Point(bounds.x, bounds.y)
    val translated = Point(event.x - inlayPoint.x, event.y - inlayPoint.y)
    container.mouseMoved(event, translated)
  }
}
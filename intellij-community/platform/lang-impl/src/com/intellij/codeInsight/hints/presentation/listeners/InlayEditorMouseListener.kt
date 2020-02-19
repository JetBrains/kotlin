// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation.listeners

import com.intellij.codeInsight.hints.PresentationContainerRenderer
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import java.awt.Point

/**
 * Global mouse listener, that provide events to inlay hints at mouse coordinates.
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
// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.util.messages.MessageBus
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent

/**
 * Global mouse listener, that provide events to inlay hints at mouse coordinates.
 */
class PresentationMouseHandler(val editorFactory: EditorFactory) {

  private var activePresentation : InlayPresentation? = null

  private val mouseListener = object: EditorMouseListener {
    override fun mouseClicked(e: EditorMouseEvent) {
      if (!e.isConsumed) {
        val editor = e.editor
        val event = e.mouseEvent
        val point = event.point
        val inlay = editor.inlayModel.getElementAt(point) ?: return
        val inlayPoint = editor.visualPositionToXY(inlay.visualPosition)
        val renderer = inlay.renderer
        if (renderer !is PresentationRenderer) return

        val copy = inlayPoint.clone() as Point
        val inPresentationCoordinatesEvent = translate(event, -inlayPoint.x, -inlayPoint.y)
        renderer.presentation.mouseClicked(inPresentationCoordinatesEvent, copy)
      }
    }

    private fun translate(e: MouseEvent, dx: Int, dy: Int): MouseEvent {
      return MouseEvent(e.source as Component, e.id, e.`when`, e.modifiers,
                        e.x + dx, e.y + dy, e.xOnScreen, e.yOnScreen, e.clickCount, e.isPopupTrigger, e.button)
    }

  }

  private val mouseMotionListener = object: EditorMouseMotionListener {
    override fun mouseMoved(e: EditorMouseEvent) {
      if (!e.isConsumed) {
        val editor = e.editor
        val event = e.mouseEvent
        // TODO here also may be handling of ESC key
        val inlay = editor.inlayModel.getElementAt(event.point)
        val presentation = (inlay?.renderer as? PresentationRenderer)?.presentation
        if (activePresentation != presentation) {
          activePresentation?.mouseExited()
          activePresentation = presentation
        }
        if (presentation != null) {
          val point = editor.visualPositionToXY(inlay.visualPosition)
          event.withTranslated(-point.x, -point.y) {
            presentation.mouseMoved(event)
          }
        }
      }
    }
  }

  init {
    val multicaster = editorFactory.eventMulticaster
    multicaster.addEditorMouseListener(mouseListener)
    multicaster.addEditorMouseMotionListener(mouseMotionListener)
  }
}
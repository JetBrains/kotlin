// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Rectangle

/**
 * Building block of inlay view.
 * It's implementations are not expected to throw exceptions.
 */
interface InlayPresentation : InputHandler {
  val width: Int
  val height: Int

  /**
   * @param g graphics to draw inlay on. Rectangle where allowed to draw: (0, 0) - (width - 1, height - 1)
   */
  fun paint(g: Graphics2D, attributes: TextAttributes)

  /**
   * Notifies listeners about the change of the size, which cause partial repaint
   */
  fun fireSizeChanged(previous: Dimension, current: Dimension)

  /**
   * Notifies listeners about any change in the content of the given area
   * @param area in the coordinate system of current inlay
   */
  fun fireContentChanged(area: Rectangle)

  fun addListener(listener: PresentationListener)

  fun removeListener(listener: PresentationListener)

  /**
   * This method is called, when pass collects new presentation at element, where old one exists.
   * After successful update some event should be fired
   * @param newPresentation presentation that was recently collected. It is guaranteed to be always exactly the same type as this instance.
   * @return true, if updated successfully
   */
  fun updateIfNecessary(newPresentation: InlayPresentation): Boolean

  /**
   * For testings purposes
   */
  override fun toString(): String
}
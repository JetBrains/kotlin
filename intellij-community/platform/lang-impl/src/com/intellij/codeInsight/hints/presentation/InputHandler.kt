// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import java.awt.Point
import java.awt.event.MouseEvent

/**
 * Note to all API: you should not rely on fact, that if some method called then mouse inside rectangle (0, 0), (width, height)
 */
interface InputHandler {
  /**
   * Called when user clicks on presentation.
   * @param e event translated into coordinates of current presentation
   * @param editorPoint original event point
   */
  fun mouseClicked(e: MouseEvent, editorPoint: Point) {}

  /**
   * Called when user moves mouse in bounds of inlay.
   * @param e event translated into coordinates of current presentation
   */
  fun mouseMoved(e: MouseEvent) {}

  /**
   * Called when mouse leaves presentation.
   */
  fun mouseExited() {}
}
// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import java.awt.event.MouseEvent

/**
 * Pure presentation. If you need to preserve state between updates, you should use [StatefulPresentation]
 */
class OnHoverPresentation(
  presentation: InlayPresentation,
  val onHover: (MouseEvent?) -> Unit // When null comes, hover is finished
) : StaticDelegatePresentation(presentation) {
  var isInside = false

  override fun mouseMoved(e: MouseEvent) {
    super.mouseMoved(e)
    if (!isInside) {
      isInside = true
      onHover(e)
    }
  }

  override fun mouseExited() {
    super.mouseExited()
    isInside = false
    onHover(null)
  }
}
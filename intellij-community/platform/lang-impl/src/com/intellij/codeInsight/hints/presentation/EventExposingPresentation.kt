// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import java.awt.Point
import java.awt.event.MouseEvent

class EventExposingPresentation(val base: InlayPresentation) : StaticDelegatePresentation(base) {
  private val listeners = hashSetOf<InputHandler>()

  fun addInputListener(listener: InputHandler) {
    listeners.add(listener)
  }

  override fun mouseClicked(event: MouseEvent, translated: Point) {
    super.mouseClicked(event, translated)
    for (listener in listeners) {
      listener.mouseClicked(event, translated)
    }
  }

  override fun mouseMoved(event: MouseEvent, translated: Point) {
    super.mouseMoved(event, translated)
    for (listener in listeners) {
      listener.mouseMoved(event, translated)
    }
  }

  override fun mouseExited() {
    super.mouseExited()
    for (listener in listeners) {
      listener.mouseExited()
    }
  }
}
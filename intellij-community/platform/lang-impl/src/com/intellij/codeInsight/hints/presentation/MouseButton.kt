// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

enum class MouseButton {
  Left,
  Middle,
  Right;

  companion object {
    @JvmStatic
    fun fromEvent(e: MouseEvent): MouseButton? = when {
      SwingUtilities.isLeftMouseButton(e) -> Left
      SwingUtilities.isMiddleMouseButton(e) -> Middle
      SwingUtilities.isRightMouseButton(e) -> Right
      else -> null
    }
  }
}

val MouseEvent.mouseButton
  get() = MouseButton.fromEvent(this)
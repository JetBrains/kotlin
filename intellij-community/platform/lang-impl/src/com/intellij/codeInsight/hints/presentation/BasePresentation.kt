// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import java.awt.Dimension
import java.awt.Rectangle

abstract class BasePresentation : InlayPresentation {
  private val listeners = hashSetOf<PresentationListener>()

  override fun fireSizeChanged(previous: Dimension, current: Dimension) {
    for (listener in listeners) {
      listener.sizeChanged(previous, current)
    }
  }
  override fun fireContentChanged(area: Rectangle) {
    for (listener in listeners) {
      listener.contentChanged(area)
    }
  }

  override fun addListener(listener: PresentationListener) {
    listeners.add(listener)
  }

  override fun removeListener(listener: PresentationListener) {
    listeners.remove(listener)
  }
}
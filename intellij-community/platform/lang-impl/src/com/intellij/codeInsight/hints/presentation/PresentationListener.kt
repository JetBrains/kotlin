// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import java.awt.Dimension
import java.awt.Rectangle

interface PresentationListener {
  fun contentChanged(area: Rectangle)

  fun sizeChanged(previous: Dimension, current: Dimension)
}
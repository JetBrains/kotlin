// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl

import com.intellij.ui.awt.RelativePoint
import java.awt.Component
import java.awt.Dimension
import java.awt.Point

interface HectorComponent {
    fun showComponent(point: RelativePoint)
    fun showComponent(component: Component, offset : (Dimension) -> Point)
}
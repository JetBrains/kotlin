/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import javax.swing.Spring
import javax.swing.SpringLayout

internal operator fun Spring.plus(other: Spring) = Spring.sum(this, other)
internal operator fun Spring.plus(gap: Int) = Spring.sum(this, Spring.constant(gap))
internal operator fun Spring.minus(other: Spring) = this + Spring.minus(other)
internal operator fun Spring.unaryMinus() = Spring.minus(this)
internal operator fun Spring.times(by: Float) = Spring.scale(this, by)
internal fun Int.asSpring() = Spring.constant(this)
internal operator fun SpringLayout.Constraints.get(edgeName: String) = getConstraint(edgeName)
internal operator fun SpringLayout.Constraints.set(edgeName: String, spring: Spring) {
    setConstraint(edgeName, spring)
}

fun springMin(s1: Spring, s2: Spring) = -Spring.max(-s1, -s2)



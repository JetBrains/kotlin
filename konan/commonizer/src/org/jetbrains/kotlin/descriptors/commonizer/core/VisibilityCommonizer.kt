/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility

abstract class VisibilityCommonizer : Commonizer<Visibility, Visibility> {

    companion object {
        fun lowering(): VisibilityCommonizer = LoweringVisibilityCommonizer()
        fun equalizing(): VisibilityCommonizer = EqualizingVisibilityCommonizer()
    }

    private var temp: Visibility? = null

    override val result: Visibility
        get() {
            return temp?.takeIf { it != Visibilities.UNKNOWN } ?: error("Visibility can't be commonized")
        }

    override fun commonizeWith(next: Visibility): Boolean {
        if (temp == Visibilities.UNKNOWN)
            return false

        if (Visibilities.isPrivate(next)) {
            temp = Visibilities.UNKNOWN
            return false
        }

        temp = temp?.let { temp -> getNext(temp, next) } ?: next

        return temp != Visibilities.UNKNOWN
    }

    protected abstract fun getNext(current: Visibility, next: Visibility): Visibility
}

private class LoweringVisibilityCommonizer : VisibilityCommonizer() {
    override fun getNext(current: Visibility, next: Visibility): Visibility {
        val comparisonResult: Int = Visibilities.compare(current, next)
            ?: return Visibilities.UNKNOWN // two visibilities that can't be compared against each one, ex: protected vs internal

        return if (comparisonResult <= 0) current else next
    }
}

private class EqualizingVisibilityCommonizer : VisibilityCommonizer() {
    override fun getNext(current: Visibility, next: Visibility) =
        if (Visibilities.compare(current, next) == 0) current else Visibilities.UNKNOWN
}

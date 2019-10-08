/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirDeclarationWithVisibility
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirFunctionOrProperty
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.isVirtual

abstract class VisibilityCommonizer(private val allowPrivate: Boolean) : Commonizer<CirDeclarationWithVisibility, Visibility> {

    companion object {
        fun lowering(allowPrivate: Boolean = false): VisibilityCommonizer = LoweringVisibilityCommonizer(allowPrivate)
        fun equalizing(): VisibilityCommonizer = EqualizingVisibilityCommonizer()
    }

    private var temp: Visibility? = null

    override val result: Visibility
        get() = temp?.takeIf { it != Visibilities.UNKNOWN } ?: throw IllegalCommonizerStateException()

    override fun commonizeWith(next: CirDeclarationWithVisibility): Boolean {
        if (temp == Visibilities.UNKNOWN)
            return false

        val nextVisibility = next.visibility
        if (!allowPrivate && Visibilities.isPrivate(nextVisibility) || !canBeCommonized(next)) {
            temp = Visibilities.UNKNOWN
            return false
        }

        temp = temp?.let { temp -> getNext(temp, nextVisibility) } ?: nextVisibility

        return temp != Visibilities.UNKNOWN
    }

    protected abstract fun canBeCommonized(next: CirDeclarationWithVisibility): Boolean
    protected abstract fun getNext(current: Visibility, next: Visibility): Visibility
}

/**
 * Choose the lowest possible visibility ignoring private for all given member descriptors, if possible.
 * If at least one member descriptor is virtual, then the commonizer succeeds only if all visibilities are equal.
 */
private class LoweringVisibilityCommonizer(allowPrivate: Boolean) : VisibilityCommonizer(allowPrivate) {
    private var atLeastOneVirtualCallableMet = false
    private var atLeastTwoVisibilitiesMet = false

    override fun canBeCommonized(next: CirDeclarationWithVisibility): Boolean {
        if (!atLeastOneVirtualCallableMet)
            atLeastOneVirtualCallableMet = (next as? CirFunctionOrProperty)?.isVirtual() == true

        return !atLeastOneVirtualCallableMet || !atLeastTwoVisibilitiesMet
    }

    override fun getNext(current: Visibility, next: Visibility): Visibility {
        val comparisonResult: Int = Visibilities.compare(current, next)
            ?: return Visibilities.UNKNOWN // two visibilities that can't be compared against each one, ex: protected vs internal

        if (!atLeastTwoVisibilitiesMet)
            atLeastTwoVisibilitiesMet = comparisonResult != 0

        if (atLeastOneVirtualCallableMet && atLeastTwoVisibilitiesMet)
            return Visibilities.UNKNOWN

        return if (comparisonResult <= 0) current else next
    }
}

/**
 * Make sure that visibilities of all member descriptors are equal are not private according to [Visibilities.isPrivate].
 */
private class EqualizingVisibilityCommonizer : VisibilityCommonizer(false) {
    override fun canBeCommonized(next: CirDeclarationWithVisibility) = true

    override fun getNext(current: Visibility, next: Visibility) =
        if (Visibilities.compare(current, next) == 0) current else Visibilities.UNKNOWN
}

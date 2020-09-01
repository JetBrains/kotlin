/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirFunctionOrProperty
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirHasVisibility

abstract class VisibilityCommonizer : Commonizer<CirHasVisibility, DescriptorVisibility> {

    companion object {
        fun lowering(): VisibilityCommonizer = LoweringVisibilityCommonizer()
        fun equalizing(): VisibilityCommonizer = EqualizingVisibilityCommonizer()
    }

    private var temp: DescriptorVisibility? = null

    override val result: DescriptorVisibility
        get() = checkState(temp, temp == DescriptorVisibilities.UNKNOWN)

    override fun commonizeWith(next: CirHasVisibility): Boolean {
        if (temp == DescriptorVisibilities.UNKNOWN)
            return false

        val nextVisibility = next.visibility
        if (DescriptorVisibilities.isPrivate(nextVisibility) || !canBeCommonized(next)) {
            temp = DescriptorVisibilities.UNKNOWN
            return false
        }

        temp = temp?.let { temp -> getNext(temp, nextVisibility) } ?: nextVisibility

        return temp != DescriptorVisibilities.UNKNOWN
    }

    protected abstract fun canBeCommonized(next: CirHasVisibility): Boolean
    protected abstract fun getNext(current: DescriptorVisibility, next: DescriptorVisibility): DescriptorVisibility
}

/**
 * Choose the lowest possible visibility ignoring private for all given member descriptors.
 * If at least one member descriptor is virtual, then the commonizer succeeds only if all visibilities are equal.
 */
private class LoweringVisibilityCommonizer : VisibilityCommonizer() {
    private var atLeastOneVirtualCallableMet = false
    private var atLeastTwoVisibilitiesMet = false

    override fun canBeCommonized(next: CirHasVisibility): Boolean {
        if (!atLeastOneVirtualCallableMet)
            atLeastOneVirtualCallableMet = (next as? CirFunctionOrProperty)?.isVirtual() == true

        return !atLeastOneVirtualCallableMet || !atLeastTwoVisibilitiesMet
    }

    override fun getNext(current: DescriptorVisibility, next: DescriptorVisibility): DescriptorVisibility {
        val comparisonResult: Int = DescriptorVisibilities.compare(current, next)
            ?: return DescriptorVisibilities.UNKNOWN // two visibilities that can't be compared against each one, ex: protected vs internal

        if (!atLeastTwoVisibilitiesMet)
            atLeastTwoVisibilitiesMet = comparisonResult != 0

        if (atLeastOneVirtualCallableMet && atLeastTwoVisibilitiesMet)
            return DescriptorVisibilities.UNKNOWN

        return if (comparisonResult <= 0) current else next
    }
}

/**
 * Make sure that visibilities of all member descriptors are equal and are not private according to [DescriptorVisibilities.isPrivate].
 */
private class EqualizingVisibilityCommonizer : VisibilityCommonizer() {
    override fun canBeCommonized(next: CirHasVisibility) = true

    override fun getNext(current: DescriptorVisibility, next: DescriptorVisibility) =
        if (DescriptorVisibilities.compare(current, next) == 0) current else DescriptorVisibilities.UNKNOWN
}

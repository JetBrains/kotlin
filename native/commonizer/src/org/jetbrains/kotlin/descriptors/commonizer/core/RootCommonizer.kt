/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirRoot
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirRootFactory

class RootCommonizer : AbstractStandardCommonizer<CirRoot, CirRoot>() {
    private val leafTargets = mutableSetOf<LeafCommonizerTarget>()

    override fun commonizationResult() = CirRootFactory.create(
        target = SharedCommonizerTarget(leafTargets)
    )

    override fun initialize(first: CirRoot) {
        leafTargets += first.target as LeafCommonizerTarget
    }

    override fun doCommonizeWith(next: CirRoot): Boolean {
        leafTargets += next.target as LeafCommonizerTarget
        return true
    }
}

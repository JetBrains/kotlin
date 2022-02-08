/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.allLeaves
import org.jetbrains.kotlin.commonizer.cir.CirRoot

class RootCommonizer : AbstractStandardCommonizer<CirRoot, CirRoot>() {
    private val targets = mutableSetOf<LeafCommonizerTarget>()

    override fun commonizationResult() = CirRoot.create(
        target = SharedCommonizerTarget(targets)
    )

    override fun initialize(first: CirRoot) {
        targets += first.target.allLeaves()
    }

    override fun doCommonizeWith(next: CirRoot): Boolean {
        targets += next.target.allLeaves()
        return true
    }
}

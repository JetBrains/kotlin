/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.commonizer.BuiltInsProvider
import org.jetbrains.kotlin.descriptors.commonizer.InputTarget
import org.jetbrains.kotlin.descriptors.commonizer.OutputTarget
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirRoot
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirRootFactory

class RootCommonizer : AbstractStandardCommonizer<CirRoot, CirRoot>() {
    private val inputTargets = mutableSetOf<InputTarget>()
    private var konanBuiltInsProvider: BuiltInsProvider? = null

    override fun commonizationResult() = CirRootFactory.create(
        target = OutputTarget(inputTargets),
        builtInsClass = if (konanBuiltInsProvider != null) KonanBuiltIns::class.java.name else DefaultBuiltIns::class.java.name,
        builtInsProvider = konanBuiltInsProvider ?: BuiltInsProvider.wrap(DefaultBuiltIns.Instance)
    )

    override fun initialize(first: CirRoot) {
        inputTargets += first.target as InputTarget
        konanBuiltInsProvider = first.konanBuiltInsProvider
    }

    override fun doCommonizeWith(next: CirRoot): Boolean {
        inputTargets += next.target as InputTarget

        // keep the first met KonanBuiltIns when all targets are Kotlin/Native
        // otherwise use DefaultBuiltIns
        if (konanBuiltInsProvider != null && next.konanBuiltInsProvider == null) {
            konanBuiltInsProvider = null
        }

        return true
    }

    private inline val CirRoot.konanBuiltInsProvider
        get() = if (builtInsClass == KonanBuiltIns::class.java.name) builtInsProvider else null
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.commonizer.BuiltInsProvider
import org.jetbrains.kotlin.descriptors.commonizer.LeafTarget
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirRoot
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirRootImpl

object CirRootFactory {
    fun create(
        target: CommonizerTarget,
        builtInsClass: String,
        builtInsProvider: BuiltInsProvider
    ): CirRoot {
        if (target is LeafTarget) {
            check((target.konanTarget != null) == (builtInsClass == KonanBuiltIns::class.java.name))
        }

        return CirRootImpl(
            target = target,
            builtInsClass = builtInsClass,
            builtInsProvider = builtInsProvider
        )
    }
}

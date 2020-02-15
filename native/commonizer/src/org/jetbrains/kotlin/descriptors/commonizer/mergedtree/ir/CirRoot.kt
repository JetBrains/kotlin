/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.commonizer.BuiltInsProvider
import org.jetbrains.kotlin.descriptors.commonizer.InputTarget
import org.jetbrains.kotlin.descriptors.commonizer.Target

data class CirRoot(
    val target: Target,
    val builtInsClass: String,
    val builtInsProvider: BuiltInsProvider
) : CirDeclaration {
    init {
        if (target is InputTarget) {
            check((target.konanTarget != null) == (builtInsClass == KonanBuiltIns::class.java.name))
        }
    }
}

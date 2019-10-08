/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.konan.target.KonanTarget

// N.B. TargetPlatform/SimplePlatform are non exhaustive enough to address both target platforms such as
// JVM, JS and concrete Kotlin/Native targets, e.g. macos_x64, ios_x64, linux_x64.
sealed class Target

data class InputTarget(val name: String, val konanTarget: KonanTarget? = null) : Target()

data class OutputTarget(val targets: Set<Target>) : Target() {
    init {
        require(targets.isNotEmpty())
    }
}

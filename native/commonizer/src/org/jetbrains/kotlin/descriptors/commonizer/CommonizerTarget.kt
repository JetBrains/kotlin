/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.konan.target.KonanTarget

// N.B. TargetPlatform/SimplePlatform are non exhaustive enough to address both target platforms such as
// JVM, JS and concrete Kotlin/Native targets, e.g. macos_x64, ios_x64, linux_x64.
sealed class CommonizerTarget {
    abstract val name: String
    abstract val prettyName: String

    fun prettyCommonizedName(sharedTarget: SharedTarget): String = when {
        this == sharedTarget -> prettyName
        this in sharedTarget.targets -> sharedTarget.targets.joinToString(prefix = "[", postfix = "]") {
            if (it == this) "${it.name}(*)" else it.name
        }
        else -> error("Target $prettyName is not in ${sharedTarget.prettyName}")
    }
}

data class LeafTarget(override val name: String, val konanTarget: KonanTarget? = null) : CommonizerTarget() {
    override val prettyName get() = "[$name]"
}

data class SharedTarget(val targets: Set<CommonizerTarget>) : CommonizerTarget() {
    init {
        require(targets.isNotEmpty())
    }

    override val name get() = targets.joinToString(prefix = "[", postfix = "]") { it.name }
    override val prettyName get() = name
}

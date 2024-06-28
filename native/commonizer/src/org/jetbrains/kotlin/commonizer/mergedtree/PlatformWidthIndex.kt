/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.allLeaves
import org.jetbrains.kotlin.commonizer.utils.singleDistinctValueOrNull
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

enum class PlatformIntWidth {
    INT, LONG, MIXED
}

object PlatformWidthIndex {
    private val widthByLeafTargets = mapOf(
        LeafCommonizerTarget(KonanTarget.IOS_ARM64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.IOS_X64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.IOS_SIMULATOR_ARM64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.WATCHOS_ARM32) to PlatformIntWidth.INT,
        LeafCommonizerTarget(KonanTarget.WATCHOS_ARM64) to PlatformIntWidth.INT,
        LeafCommonizerTarget(KonanTarget.WATCHOS_X64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.WATCHOS_SIMULATOR_ARM64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.WATCHOS_DEVICE_ARM64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.TVOS_ARM64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.TVOS_X64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.TVOS_SIMULATOR_ARM64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.LINUX_X64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.MINGW_X64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.MACOS_X64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.MACOS_ARM64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.LINUX_ARM64) to PlatformIntWidth.LONG,
        LeafCommonizerTarget(KonanTarget.LINUX_ARM32_HFP) to PlatformIntWidth.INT,
    )

    fun platformWidthOf(target: CommonizerTarget): PlatformIntWidth? {
        return when (target) {
            is LeafCommonizerTarget -> widthByLeafTargets[target]
            is SharedCommonizerTarget -> target.allLeaves().toList().let { leafTargets ->
                val sameForAllLeaves = leafTargets.singleDistinctValueOrNull { platformWidthOf(it) }
                if (sameForAllLeaves != null) return@let sameForAllLeaves

                leafTargets.all { platformWidthOf(it) != null }.ifTrue { PlatformIntWidth.MIXED }
            }
        }
    }
}

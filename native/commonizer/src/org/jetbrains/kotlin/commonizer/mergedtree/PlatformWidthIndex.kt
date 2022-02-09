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

enum class PlatformWidth(val isKnown: Boolean) {
    INT(true), LONG(true), MIXED(true), UNKNOWN(false)
}

interface PlatformWidthIndex {
    fun platformWidthOf(target: CommonizerTarget): PlatformWidth
}

object PlatformWidthIndexImpl : PlatformWidthIndex {
    private val widthByLeafTargets = mapOf(
        LeafCommonizerTarget(KonanTarget.IOS_ARM32) to PlatformWidth.INT,
        LeafCommonizerTarget(KonanTarget.IOS_ARM64) to PlatformWidth.LONG,
        LeafCommonizerTarget(KonanTarget.IOS_X64) to PlatformWidth.LONG,
        LeafCommonizerTarget(KonanTarget.IOS_SIMULATOR_ARM64) to PlatformWidth.LONG,
        LeafCommonizerTarget(KonanTarget.WATCHOS_ARM32) to PlatformWidth.INT,
        LeafCommonizerTarget(KonanTarget.WATCHOS_ARM64) to PlatformWidth.INT,
        LeafCommonizerTarget(KonanTarget.WATCHOS_X86) to PlatformWidth.INT,
        LeafCommonizerTarget(KonanTarget.WATCHOS_X64) to PlatformWidth.LONG,
        LeafCommonizerTarget(KonanTarget.WATCHOS_SIMULATOR_ARM64) to PlatformWidth.LONG,
        LeafCommonizerTarget(KonanTarget.TVOS_ARM64) to PlatformWidth.LONG,
        LeafCommonizerTarget(KonanTarget.TVOS_X64) to PlatformWidth.LONG,
        LeafCommonizerTarget(KonanTarget.TVOS_SIMULATOR_ARM64) to PlatformWidth.LONG,
        LeafCommonizerTarget(KonanTarget.LINUX_X64) to PlatformWidth.LONG,
        LeafCommonizerTarget(KonanTarget.MINGW_X86) to PlatformWidth.INT,
        LeafCommonizerTarget(KonanTarget.MINGW_X64) to PlatformWidth.LONG,
        LeafCommonizerTarget(KonanTarget.MACOS_X64) to PlatformWidth.LONG,
        LeafCommonizerTarget(KonanTarget.MACOS_ARM64) to PlatformWidth.LONG,
        LeafCommonizerTarget(KonanTarget.LINUX_ARM64) to PlatformWidth.LONG,
        LeafCommonizerTarget(KonanTarget.LINUX_ARM32_HFP) to PlatformWidth.INT,
        LeafCommonizerTarget(KonanTarget.LINUX_MIPS32) to PlatformWidth.INT,
        LeafCommonizerTarget(KonanTarget.LINUX_MIPSEL32) to PlatformWidth.INT,
        LeafCommonizerTarget(KonanTarget.WASM32) to PlatformWidth.INT,
    )

    override fun platformWidthOf(target: CommonizerTarget): PlatformWidth {
        return when (target) {
            is LeafCommonizerTarget -> widthByLeafTargets[target]
                ?: PlatformWidth.UNKNOWN
            is SharedCommonizerTarget -> target.allLeaves().toList().let { leafTargets ->
                leafTargets.singleDistinctValueOrNull { platformWidthOf(it) }
                    ?: if (leafTargets.all { platformWidthOf(it).isKnown })
                        PlatformWidth.MIXED
                    else PlatformWidth.UNKNOWN
            }
        }
    }
}


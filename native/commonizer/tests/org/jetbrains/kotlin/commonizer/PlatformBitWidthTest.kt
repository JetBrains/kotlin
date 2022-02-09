/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.mergedtree.PlatformWidth
import org.jetbrains.kotlin.commonizer.mergedtree.PlatformWidthIndex
import org.jetbrains.kotlin.commonizer.mergedtree.PlatformWidthIndexImpl
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Test
import kotlin.test.assertEquals

class PlatformBitWidthTest {
    private fun createPlatformBitWidthIndex(): PlatformWidthIndex = PlatformWidthIndexImpl

    @Test
    fun `test leaf int`() {
        assertEquals(
            PlatformWidth.INT,
            createPlatformBitWidthIndex().platformWidthOf(CommonizerTarget(KonanTarget.IOS_ARM32))
        )
    }

    @Test
    fun `test leaf long`() {
        assertEquals(
            PlatformWidth.LONG,
            createPlatformBitWidthIndex().platformWidthOf(CommonizerTarget(KonanTarget.MACOS_X64))
        )
    }

    @Test
    fun `test shared int`() {
        assertEquals(
            PlatformWidth.INT,
            createPlatformBitWidthIndex().platformWidthOf(CommonizerTarget(KonanTarget.IOS_ARM32, KonanTarget.LINUX_MIPS32))
        )
    }

    @Test
    fun `test shared long`() {
        assertEquals(
            PlatformWidth.LONG,
            createPlatformBitWidthIndex().platformWidthOf(CommonizerTarget(KonanTarget.MACOS_X64, KonanTarget.LINUX_X64))
        )
    }

    @Test
    fun `test int and long`() {
        assertEquals(
            PlatformWidth.MIXED,
            createPlatformBitWidthIndex().platformWidthOf(CommonizerTarget(KonanTarget.IOS_ARM32, KonanTarget.MACOS_X64))
        )
    }

    @Test
    fun `test int and unknown`() {
        assertEquals(
            PlatformWidth.UNKNOWN,
            createPlatformBitWidthIndex().platformWidthOf(
                CommonizerTarget(
                    LeafCommonizerTarget(KonanTarget.IOS_ARM32),
                    LeafCommonizerTarget("unknown_target")
                )
            )
        )
    }

    @Test
    fun `test long and unknown`() {
        assertEquals(
            PlatformWidth.UNKNOWN,
            createPlatformBitWidthIndex().platformWidthOf(
                CommonizerTarget(
                    LeafCommonizerTarget(KonanTarget.MACOS_X64),
                    LeafCommonizerTarget("unknown_target")
                )
            )
        )
    }

    @Test
    fun `test the good, the bad and the ugly`() {
        assertEquals(
            PlatformWidth.UNKNOWN,
            createPlatformBitWidthIndex().platformWidthOf(
                CommonizerTarget(
                    LeafCommonizerTarget(KonanTarget.MACOS_X64),
                    LeafCommonizerTarget(KonanTarget.IOS_ARM32),
                    LeafCommonizerTarget("unknown_target"),
                )
            )
        )
    }

    @Test
    fun `test watchosArm64 is considered int`() {
        assertEquals(
            PlatformWidth.INT,
            createPlatformBitWidthIndex().platformWidthOf(CommonizerTarget(KonanTarget.WATCHOS_ARM64))
        )
    }
}

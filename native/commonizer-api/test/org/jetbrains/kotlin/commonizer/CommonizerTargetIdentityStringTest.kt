/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import kotlin.test.*

class CommonizerTargetIdentityStringTest {

    @Test
    fun leafTargets() {
        KonanTarget.predefinedTargets.values.forEach { konanTarget ->
            assertEquals(konanTarget.name, CommonizerTarget(konanTarget).identityString)
            assertEquals(CommonizerTarget(konanTarget), parseCommonizerTarget(CommonizerTarget(konanTarget).identityString))
        }
    }

    @Test
    fun `simple shared targets are invariant under konanTarget order`() {
        val macosFirst = CommonizerTarget(MACOS_X64, LINUX_X64)
        val linuxFirst = CommonizerTarget(LINUX_X64, MACOS_X64)

        assertEquals(macosFirst, linuxFirst)
        assertEquals(macosFirst.identityString, linuxFirst.identityString)
        assertEquals(linuxFirst, parseCommonizerTarget(linuxFirst.identityString))
        assertEquals(macosFirst, parseCommonizerTarget(macosFirst.identityString))
        assertEquals(macosFirst, parseCommonizerTarget(linuxFirst.identityString))
        assertEquals(linuxFirst, parseCommonizerTarget(macosFirst.identityString))
    }

    @Test
    fun `hierarchical commonizer targets`() {
        val hierarchy = SharedCommonizerTarget(
            CommonizerTarget(LINUX_X64, MACOS_X64),
            CommonizerTarget(IOS_ARM64, IOS_X64)
        )
        assertEquals(setOf(LINUX_X64, MACOS_X64, IOS_ARM64, IOS_X64), hierarchy.konanTargets)
        assertEquals(hierarchy, parseCommonizerTarget(hierarchy.identityString))
    }

    @Test
    fun `multilevel hierarchical commonizer targets`() {
        val hierarchy = SharedCommonizerTarget(
            SharedCommonizerTarget(
                SharedCommonizerTarget(
                    SharedCommonizerTarget(
                        CommonizerTarget(LINUX_X64, MACOS_X64),
                        CommonizerTarget(IOS_X64, IOS_ARM64)
                    ),
                    CommonizerTarget(LINUX_ARM32_HFP)
                ),
                CommonizerTarget(LINUX_MIPSEL32)
            ),
            CommonizerTarget(WATCHOS_X86, WATCHOS_ARM64)
        )

        assertEquals(hierarchy, parseCommonizerTarget(hierarchy.identityString))
    }

    @Test
    fun `parsing CommonizerTarget`() {
        val target = parseCommonizerTarget("(x, (x, y, (a, b), (b, c)))")
        assertEquals(
            SharedCommonizerTarget(
                LeafCommonizerTarget("x"),
                SharedCommonizerTarget(
                    LeafCommonizerTarget("x"),
                    LeafCommonizerTarget("y"),
                    SharedCommonizerTarget(
                        LeafCommonizerTarget("a"),
                        LeafCommonizerTarget("b"),
                    ),
                    SharedCommonizerTarget(
                        LeafCommonizerTarget("b"),
                        LeafCommonizerTarget("c")
                    )
                )
            ),
            target
        )
    }

    @Test
    fun `empty shared target`() {
        assertEquals(SharedCommonizerTarget(emptySet<CommonizerTarget>()), parseCommonizerTarget("()"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fail parsing CommonizerTarget 1`() {
        parseCommonizerTarget("xxx,")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fail parsing CommonizerTarget 2`() {
        parseCommonizerTarget("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fail parsing CommonizerTarget 4`() {
        parseCommonizerTarget("(xxx")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fail parsing CommonizerTarget 5`() {
        parseCommonizerTarget("xxx)")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fail parsing CommonizerTarget 6`() {
        parseCommonizerTarget("(xxx")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fail parsing CommonizerTarget 7`() {
        parseCommonizerTarget("(xxx yyy)")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fail parsing CommonizerTarget 8`() {
        parseCommonizerTarget(" ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fail parsing CommonizerTarget 9`() {
        parseCommonizerTarget("xxx?")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fail parsing CommonizerTarget 10`() {
        parseCommonizerTarget("(x, (x, y)")
    }

    @Test
    fun isCommonizerIdentityString() {
        assertFalse(isCommonizerTargetIdentityString(""))
        assertTrue(isCommonizerTargetIdentityString("()"))
        assertTrue(isCommonizerTargetIdentityString("(a,b)"))
        assertFalse(isCommonizerTargetIdentityString("..."))
        assertTrue(isCommonizerTargetIdentityString("x"))
        assertFalse(isCommonizerTargetIdentityString("((a)"))
    }

    @Test
    fun parseCommonizerTargetOrNull() {
        assertEquals(parseCommonizerTarget("((a,b),c)"), parseCommonizerTargetOrNull("((a,b),c)"))
        assertNull(parseCommonizerTargetOrNull(""))
    }
}

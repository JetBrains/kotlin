/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Test
import kotlin.test.assertEquals

class CommonizerTargetTest {

    @Test
    fun leafTargetNames() {
        listOf(
            Triple("foo", "[foo]", FOO),
            Triple("bar", "[bar]", BAR),
            Triple("baz_123", "[baz_123]", BAZ),
        ).forEach { (name, prettyName, target: LeafTarget) ->
            assertEquals(name, target.name)
            assertEquals(prettyName, target.prettyName)
        }
    }

    @Test
    fun sharedTargetNames() {
        listOf(
            "[foo]" to SharedTarget(FOO),
            "[foo, bar]" to SharedTarget(FOO, BAR),
            "[foo, bar, baz_123]" to SharedTarget(FOO, BAR, BAZ),
            "[foo, bar, baz_123, [foo, bar]]" to SharedTarget(FOO, BAR, BAZ, SharedTarget(FOO, BAR))
        ).forEach { (prettyName, target: SharedTarget) ->
            assertEquals(prettyName, target.prettyName)
            assertEquals(prettyName, target.name)
        }
    }

    @Test
    fun prettyCommonizedName() {
        val sharedTarget = SharedTarget(FOO, BAR, BAZ)
        listOf(
            "[foo(*), bar, baz_123]" to FOO,
            "[foo, bar(*), baz_123]" to BAR,
            "[foo, bar, baz_123(*)]" to BAZ,
            "[foo, bar, baz_123]" to sharedTarget
        ).forEach { (prettyCommonizerName, target: CommonizerTarget) ->
            assertEquals(prettyCommonizerName, target.prettyCommonizedName(sharedTarget))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun prettyCommonizedNameFailure() {
        FOO.prettyCommonizedName(SharedTarget(BAR, BAZ))
    }

    @Test(expected = IllegalArgumentException::class)
    fun sharedTargetNoInnerTargets() {
        SharedTarget(emptySet())
    }

    private companion object {
        val FOO = LeafTarget("foo")
        val BAR = LeafTarget("bar", KonanTarget.IOS_X64)
        val BAZ = LeafTarget("baz_123", KonanTarget.MACOS_X64)

        @Suppress("TestFunctionName")
        fun SharedTarget(vararg targets: CommonizerTarget) = SharedTarget(linkedSetOf(*targets))
    }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class IsHostSpecificKonanTargetsSetTest {

    private val hostManager = HostManager()

    @Test
    fun `partially supported hosts do not change shared metadata classification`() {
        assertFalse(KonanTarget.LINUX_ARM64 in hostManager.hostsWithFullTargetSet)
        assertEquals(
            hostManager.enabledByHost.getValue(KonanTarget.LINUX_X64),
            KonanTarget.predefinedTargets.values.filterNot {
                isHostSpecificKonanTargetsSet(listOf(it))
            }.toSet(),
        )
    }

    @Test
    fun `matches full host classification`() {
        fun fullHostImplementation(konanTargets: Iterable<KonanTarget>): Boolean {
            val enabledByHost = hostManager.enabledByHost.filterKeys { it in hostManager.hostsWithFullTargetSet }
            val allHosts = enabledByHost.keys
            fun canBeBuiltOnHosts(konanTarget: KonanTarget) = enabledByHost.filterValues { konanTarget in it }.keys
            return konanTargets.flatMapTo(mutableSetOf(), ::canBeBuiltOnHosts) != allHosts
        }

        val testInputs = sequence {
            yield(emptyList())

            KonanTarget.predefinedTargets.values.forEach { first ->
                yield(listOf(first))

                KonanTarget.predefinedTargets.values.forEach { second ->
                    yield(listOf(first, second))

                    KonanTarget.predefinedTargets.values.forEach { third ->
                        yield(listOf(first, second, third))
                    }
                }
            }
        }.toList()

        testInputs.forEach { targets ->
            assertEquals(
                fullHostImplementation(targets), isHostSpecificKonanTargetsSet(targets),
                "Unexpected host-specific classification for $targets"
            )
        }
    }
}

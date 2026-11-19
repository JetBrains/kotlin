/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KotlinNativeHostBridgeTest {

    private val supportedHosts = listOf(
        KotlinNativeHost.of(KotlinNativeHostOs.MACOS, KotlinNativeHostArchitecture.ARM64) to KonanTarget.MACOS_ARM64,
        KotlinNativeHost.of(KotlinNativeHostOs.LINUX, KotlinNativeHostArchitecture.X64) to KonanTarget.LINUX_X64,
        KotlinNativeHost.of(KotlinNativeHostOs.WINDOWS, KotlinNativeHostArchitecture.X64) to KonanTarget.MINGW_X64,
    )

    @Test
    fun `konanTarget maps the host target name to a KonanTarget`() {
        supportedHosts.forEach { (host, target) -> assertEquals(target, host.konanTarget, host.name) }
        assertNull(KotlinNativeHost.of(KotlinNativeHostOs.LINUX, KotlinNativeHostArchitecture.ARM64).konanTarget)
    }

    @Test
    fun `supported hosts match HostManager`() {
        val expected = HostManager().enabledByHost.keys
        assertEquals(expected, supportedHosts.map { it.second }.toSet(), "a host was added to or removed from HostManager")
    }

    @Test
    fun `canLinkBinaries agrees with HostManager on every supported host`() {
        val enabledByHost = HostManager().enabledByHost
        supportedHosts.forEach { (host, hostTarget) ->
            val actual = KonanTarget.predefinedTargets.values.filterTo(mutableSetOf()) { host.canLinkBinaries(it) }
            assertEquals(enabledByHost.getValue(hostTarget), actual, "linkable targets on ${host.name}")
        }
    }

    @Test
    fun `every predefined target name is known to the library table`() {
        // A target that HostManager enables somewhere must be linkable from at least one host in the library too.
        val mac = supportedHosts.first().first
        HostManager().enabledByHost.values.flatten().toSet().forEach { target ->
            assertTrue(mac.canLinkBinaries(target), "${target.name} missing from the native-host table")
        }
    }

    @Test
    fun `canRunBinaries bridge delegates by target name`() {
        val mac = supportedHosts.first().first
        assertTrue(mac.canRunBinaries(KonanTarget.MACOS_ARM64))
        assertTrue(mac.canRunBinaries(KonanTarget.IOS_SIMULATOR_ARM64))
        assertFalse(mac.canRunBinaries(KonanTarget.IOS_X64))
        assertFalse(mac.canRunBinaries(KonanTarget.LINUX_X64))
    }
}

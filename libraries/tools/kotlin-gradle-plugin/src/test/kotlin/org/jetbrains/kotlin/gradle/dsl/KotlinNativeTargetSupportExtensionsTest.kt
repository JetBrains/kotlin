/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.buildtools.nativehost.DelicateKotlinNativeHostApi
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeTargetSupport
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(DelicateKotlinNativeHostApi::class, ExperimentalKotlinGradlePluginApi::class)
class KotlinNativeTargetSupportExtensionsTest {

    private val support = KotlinNativeTargetSupport.bundled()

    @Test
    fun `konanTarget of a supported host is its entry in HostManager`() {
        assertEquals(HostManager().enabledByHost.keys, support.supportedHosts.map { support.konanTarget(it) }.toSet())
    }

    @Test
    fun `konanTarget of an unsupported host is null`() {
        assertNull(support.konanTarget(KotlinNativeHost.of(KotlinNativeHostOs.LINUX, KotlinNativeHostArchitecture.RISCV64)))
        assertNull(support.konanTarget(KotlinNativeHost.of(KotlinNativeHostOs.UNKNOWN, KotlinNativeHostArchitecture.UNKNOWN)))
    }

    @Test
    fun `canLinkBinaries agrees with HostManager for every KonanTarget`() {
        val enabledByHost = HostManager().enabledByHost
        support.supportedHosts.forEach { host ->
            val linkable = KonanTarget.predefinedTargets.values.filterTo(mutableSetOf()) { support.canLinkBinaries(host, it) }
            assertEquals(enabledByHost.getValue(support.konanTarget(host)!!), linkable, "linkable targets on ${host.name}")
        }
    }
}

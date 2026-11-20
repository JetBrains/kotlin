/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExternalToolchainDsl::class)

package org.jetbrains.kotlin.gradle.toolchains

import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.ExternalToolchainPlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ExternalToolchainPlatformTest {
    @Test
    fun `platform identity includes both operating system and architecture`() {
        val platform = ExternalToolchainPlatform("linux", "x86_64")
        val equivalent = ExternalToolchainPlatform("linux", "x86_64")

        assertEquals(platform, equivalent)
        assertEquals(platform.hashCode(), equivalent.hashCode())
        assertEquals(1, setOf(platform, equivalent).size)
        assertNotEquals(platform, ExternalToolchainPlatform("windows", "x86_64"))
        assertNotEquals(platform, ExternalToolchainPlatform("linux", "aarch64"))
        assertEquals("linux-x86_64", platform.toString())
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExternalToolchainDsl::class)

package org.jetbrains.kotlin.gradle.toolchains

import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.ExternalToolchainPlatform
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.withModifiedSystemProperties
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Isolated("Modifies system properties")
class ExternalToolchainPlatformDetectionTest {
    @Test
    fun `detects supported architecture aliases`() {
        val providers = buildProject().providers
        for ((arch, expected) in listOf("amd64" to "x86_64", "x86_64" to "x86_64", "aarch64" to "aarch64", "arm64" to "aarch64")) {
            withModifiedSystemProperties("os.name" to "Linux", "os.arch" to arch) {
                assertEquals(ExternalToolchainPlatform("linux", expected), providers.detectExternalToolchainPlatform().get())
            }
        }
    }

    @Test
    fun `rejects unsupported host architectures`() {
        val providers = buildProject().providers
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "x86") {
            val failure = assertFailsWith<IllegalArgumentException> { providers.detectExternalToolchainPlatform().get() }
            assertEquals("Unsupported OS architecture: 'x86'", failure.message)
        }
    }
}

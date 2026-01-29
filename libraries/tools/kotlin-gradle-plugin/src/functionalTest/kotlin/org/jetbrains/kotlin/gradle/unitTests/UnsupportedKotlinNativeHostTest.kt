/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.supportedHosts
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertContainsNoTaskWithName
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableCInteropCommonization
import org.jetbrains.kotlin.gradle.util.withModifiedSystemProperties
import org.jetbrains.kotlin.konan.target.HostManager
import kotlin.test.Test
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.assertEquals

@Isolated("Modifies system properties")
class UnsupportedKotlinNativeHostTest {

    @Test
    fun `test jvm project configuration`() {
        with(buildProjectWithMPP()) {
            configureRepositoriesForTests()
            multiplatformExtension.jvm()
            evaluate()
            assertNoDiagnostics(KotlinToolingDiagnostics.NativeHostNotSupportedError)
        }
    }

    @Test
    fun `test project configuration on Linux Arm64 host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "aarch64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Windows Arm64 host`() {
        withModifiedSystemProperties("os.name" to "Windows", "os.arch" to "arm64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Linux X64 host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertNoDiagnostics(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Windows X64 host`() {
        withModifiedSystemProperties("os.name" to "Windows", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertNoDiagnostics(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on FreeBSD host`() {
        withModifiedSystemProperties("os.name" to "FreeBSD", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Linux RISC-V host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "riscv64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Linux MIPS host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "mips64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Solaris host`() {
        withModifiedSystemProperties("os.name" to "SunOS", "os.arch" to "x86_64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Linux PowerPC host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "ppc64le") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test all supported hosts have explicit formatted names`() {
        // This test ensures that the formattedHostName switch is exhaustive for all known hosts.
        // If a new host is added to HostManager.enabledByHost but not to the switch,
        // it will fall back to visibleName (e.g., "linux_x64") instead of a human-readable format.
        val expectedFormattedNames = setOf(
            "Linux (x86_64)",
            "Windows (x86_64)",
            "macOS (x86_64)",
            "macOS (arm64)",
        )

        val hostManager = HostManager()
        val actualFormattedNames = hostManager.supportedHosts.toSet()

        // Verify counts match - if a new host is added, this will fail
        assertEquals(
            actualFormattedNames.size, expectedFormattedNames.size, "Number of supported hosts changed. Expected: ${expectedFormattedNames.size}, Actual: ${actualFormattedNames.size}. " +
                    "Please update the formattedHostName switch in KotlinNativeTargetPreset.kt. " +
                    "Actual hosts: $actualFormattedNames"
        )

        // Verify all expected names are present
        assertEquals(
            actualFormattedNames, expectedFormattedNames, "Supported host names don't match expected formatted names. " +
                    "Expected: $expectedFormattedNames, Actual: $actualFormattedNames. " +
                    "Please update the formattedHostName switch in KotlinNativeTargetPreset.kt."
        )
    }

    @Test
    fun `test commonizer tasks not present on FreeBSD host`() {
        withModifiedSystemProperties("os.name" to "FreeBSD", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                enableCInteropCommonization()
                multiplatformExtension.linuxX64 {
                    compilations.getByName("main").cinterops.create("dummy")
                }
                evaluate()
                assertContainsNoTaskWithName("commonizeNativeDistribution")
                assertContainsNoTaskWithName("commonizeCInterop")
            }
        }
    }
}

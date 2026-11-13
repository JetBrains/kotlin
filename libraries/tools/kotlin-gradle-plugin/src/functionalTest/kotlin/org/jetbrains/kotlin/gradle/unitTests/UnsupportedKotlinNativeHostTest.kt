/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
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
import kotlin.test.assertNull

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
    fun `test project configuration on Linux Arm64 host requires opt in`() {
        withModifiedSystemProperties(
            "os.name" to "Linux",
            "os.arch" to "aarch64",
            "kotlin.native.experimental.linuxArm64Host" to "false",
        ) {
            with(buildProjectWithMPP()) {
                plugins.apply("maven-publish")
                configureRepositoriesForTests()
                multiplatformExtension.linuxArm64()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
                val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(path)
                val expectedSolution = "To enable experimental Linux ARM64 host support, set " +
                        "'kotlin.native.experimental.linuxArm64Host=true' as a JVM system property or " +
                        "'KOTLIN_NATIVE_EXPERIMENTAL_LINUX_ARM64_HOST=true' in the environment. " +
                        "Provide a local ARM compiler through 'kotlin.native.home' until a prebuilt archive is available."
                for (factory in listOf(
                    KotlinToolingDiagnostics.NativeHostNotSupportedError,
                    KotlinToolingDiagnostics.PublishingDisabledOnUnsupportedHost,
                )) {
                    assertEquals(expectedSolution, diagnostics.single { it.id == factory.id }.solutions.first())
                }
                val warnings = diagnostics.filter { it.id.startsWith("${KotlinToolingDiagnostics.DisabledNativeTargetTaskWarning.id}_") }
                fun reasonFor(taskName: String) = warnings.single {
                    it.id == "${KotlinToolingDiagnostics.DisabledNativeTargetTaskWarning.id}_$taskName"
                }.message.lineSequence().single { it.startsWith("Reason: ") }.removePrefix("Reason: ")
                assertEquals(
                    "experimental Linux ARM64 host support is disabled. " +
                            "Set the JVM system property 'kotlin.native.experimental.linuxArm64Host=true' " +
                            "and provide a local ARM compiler through 'kotlin.native.home'.",
                    reasonFor("linuxArm64Test"),
                )
                assertEquals("tests can only run on linux_x64", reasonFor("linuxX64Test"))
            }
        }

        withModifiedSystemProperties(
            "os.name" to "Linux",
            "os.arch" to "aarch64",
            "kotlin.native.experimental.linuxArm64Host" to "true",
        ) {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxArm64()
                evaluate()
                assertNoDiagnostics(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `legacy native compiler download is skipped on unsupported Linux Arm64 host`() {
        withModifiedSystemProperties(
            "os.name" to "Linux",
            "os.arch" to "aarch64",
            "kotlin.native.experimental.linuxArm64Host" to "false",
        ) {
            with(buildProjectWithMPP(preApplyCode = {
                extensions.extraProperties.set("kotlin.native.toolchain.enabled", "false")
            })) {
                assertNull(nativeProperties.userProvidedNativeHome.orNull)
                configureRepositoriesForTests()
                assertEquals("linuxArm64", multiplatformExtension.linuxArm64().name)
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
                plugins.apply("maven-publish")
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
                val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(path)
                assertEquals(
                    "Run your build on one of the supported host platforms (listed above).",
                    diagnostics.single { it.id == KotlinToolingDiagnostics.NativeHostNotSupportedError.id }.solutions.first(),
                )
                assertEquals(
                    "Run the publish task on one of the supported host platforms (listed above).",
                    diagnostics.single { it.id == KotlinToolingDiagnostics.PublishingDisabledOnUnsupportedHost.id }.solutions.first(),
                )
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
            "Linux (arm64)",
            "Windows (x86_64)",
            "macOS (arm64)",
        )

        withModifiedSystemProperties(
            "os.name" to "Linux",
            "os.arch" to "aarch64",
            "kotlin.native.experimental.linuxArm64Host" to "true",
        ) {
            assertEquals(expectedFormattedNames, HostManager().supportedHosts.toSet())
        }
        withModifiedSystemProperties(
            "os.name" to "Linux",
            "os.arch" to "aarch64",
            "kotlin.native.experimental.linuxArm64Host" to "false",
        ) {
            assertEquals(expectedFormattedNames - "Linux (arm64)", HostManager().supportedHosts.toSet())
        }
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

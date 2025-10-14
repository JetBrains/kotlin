/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.STABLE_NATIVE_VERSION
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.setUpKotlinNativeToolchain
import org.jetbrains.kotlin.gradle.util.withModifiedSystemProperties
import org.junit.Test
import org.junit.jupiter.api.parallel.Isolated

@Isolated("Modifies system properties")
class UnsupportedKotlinNativeHostTest {

    @Test
    fun `test project configuration on Linux Arm64 host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "aarch64") {
            with(setupProject()) {
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeBundleArtifactNotFoundError)
                assertNoDiagnostics(KotlinToolingDiagnostics.NativeBundleVersionNotFoundError)
            }
        }
    }

    @Test
    fun `test project configuration on Windows Arm64 host`() {
        withModifiedSystemProperties("os.name" to "Windows", "os.arch" to "arm64") {
            with(setupProject()) {
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeBundleArtifactNotFoundError)
                assertNoDiagnostics(KotlinToolingDiagnostics.NativeBundleVersionNotFoundError)
            }
        }
    }

    @Test
    fun `test project configuration on Linux X64 host and not existing native version`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            with(setupProject("1.9.9999")) {
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeBundleVersionNotFoundError)
                assertNoDiagnostics(KotlinToolingDiagnostics.NativeBundleArtifactNotFoundError)
            }
        }
    }

    @Test
    fun `test project configuration on Linux X64 host and misconfigured repository`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            with(setupProject(addRepositories = false)) {
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeBundleVersionNotFoundError)
                assertNoDiagnostics(KotlinToolingDiagnostics.NativeBundleArtifactNotFoundError)
            }
        }
    }

//    @Test
//    fun `test project configuration on FreeBSD host`() {
//        withModifiedSystemProperties("os.name" to "FreeBSD", "os.arch" to "amd64") {
//            with(setupProject()) {
//                evaluate()
//                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeBundleResolutionUnsupportedHost)
//                assertNoDiagnostics(KotlinToolingDiagnostics.NativeBundleResolutionConfigurationError)
//            }
//        }
//    }
//
//    @Test
//    fun `test project configuration on Linux RISC-V host`() {
//        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "riscv64") {
//            with(setupProject()) {
//                evaluate()
//                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeBundleResolutionUnsupportedHost)
//                assertNoDiagnostics(KotlinToolingDiagnostics.NativeBundleResolutionConfigurationError)
//            }
//        }
//    }
//
//    @Test
//    fun `test project configuration on Linux MIPS host`() {
//        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "mips64") {
//            with(setupProject()) {
//                evaluate()
//                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeBundleResolutionUnsupportedHost)
//                assertNoDiagnostics(KotlinToolingDiagnostics.NativeBundleResolutionConfigurationError)
//            }
//        }
//    }
//
//    @Test
//    fun `test project configuration on Solaris host`() {
//        withModifiedSystemProperties("os.name" to "SunOS", "os.arch" to "x86_64") {
//            with(setupProject()) {
//                evaluate()
//                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeBundleResolutionUnsupportedHost)
//                assertNoDiagnostics(KotlinToolingDiagnostics.NativeBundleResolutionConfigurationError)
//            }
//        }
//    }
//
//    @Test
//    fun `test project configuration on Linux PowerPC host`() {
//        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "ppc64le") {
//            with(setupProject()) {
//                evaluate()
//                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeBundleResolutionUnsupportedHost)
//                assertNoDiagnostics(KotlinToolingDiagnostics.NativeBundleResolutionConfigurationError)
//            }
//        }
//    }
}

private fun setupProject(version: String = STABLE_NATIVE_VERSION, addRepositories: Boolean = true): ProjectInternal =
    buildProjectWithMPP(preApplyCode = {
        setUpKotlinNativeToolchain(version)
    }) {
        if (addRepositories) {
            configureRepositoriesForTests()
        }
        project.multiplatformExtension.linuxX64()
    }
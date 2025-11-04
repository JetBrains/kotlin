/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.withModifiedSystemProperties
import org.junit.Test
import org.junit.jupiter.api.parallel.Isolated

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
}
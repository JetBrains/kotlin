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
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.Test

@Isolated("Modifies system properties")
class DeprecatedNativeHostTest {

    @Test
    fun `test warning on macOS x64 host with native target`() {
        withModifiedSystemProperties("os.name" to "Mac OS X", "os.arch" to "x86_64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.DeprecatedNativeHostDiagnostic)
            }
        }
    }

    @Test
    fun `test no warning on macOS arm64 host with native target`() {
        withModifiedSystemProperties("os.name" to "Mac OS X", "os.arch" to "aarch64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertNoDiagnostics(KotlinToolingDiagnostics.DeprecatedNativeHostDiagnostic)
            }
        }
    }

    @Test
    fun `test no warning on macOS x64 host without native target`() {
        withModifiedSystemProperties("os.name" to "Mac OS X", "os.arch" to "x86_64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.jvm()
                evaluate()
                assertNoDiagnostics(KotlinToolingDiagnostics.DeprecatedNativeHostDiagnostic)
            }
        }
    }

    @Test
    fun `test no warning on Linux x64 host with native target`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertNoDiagnostics(KotlinToolingDiagnostics.DeprecatedNativeHostDiagnostic)
            }
        }
    }

    @Test
    fun `test no warning on Windows x64 host with native target`() {
        withModifiedSystemProperties("os.name" to "Windows", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertNoDiagnostics(KotlinToolingDiagnostics.DeprecatedNativeHostDiagnostic)
            }
        }
    }
}

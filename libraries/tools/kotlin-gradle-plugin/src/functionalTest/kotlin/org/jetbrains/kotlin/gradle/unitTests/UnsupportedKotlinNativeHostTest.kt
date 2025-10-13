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
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.setUpKotlinNativeToolchainWithStableVersion
import org.jetbrains.kotlin.gradle.util.withModifiedSystemProperties
import org.junit.Test
import org.junit.jupiter.api.parallel.Isolated

@Isolated("Modifies system properties")
class UnsupportedKotlinNativeHostTest {

    @Test
    fun `test project configuration on Linux Arm64 host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "aarch64") {
            with(setupProject()) {
                verifyUnsupportedHostWarningIsPresent()
            }
        }
    }

    @Test
    fun `test project configuration on Windows Arm64 host`() {
        withModifiedSystemProperties("os.name" to "Windows", "os.arch" to "arm64") {
            with(setupProject()) {
                verifyUnsupportedHostWarningIsPresent()
            }
        }
    }
}

private fun setupProject(): ProjectInternal = buildProjectWithMPP(preApplyCode = {
    setUpKotlinNativeToolchainWithStableVersion()
}) {
    configureRepositoriesForTests()
    project.multiplatformExtension.linuxX64()
}

private fun ProjectInternal.verifyUnsupportedHostWarningIsPresent() {
    evaluate()
    assertContainsDiagnostic(KotlinToolingDiagnostics.NativeBundleResolution)
}
/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeEnvironment
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.konan.target.KonanTarget
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Validates that [XcodeEnvironment.targets] honors `EXCLUDED_ARCHS`. This is the contract
 * the `XcodeArchitectureNotConfiguredInGradle` diagnostic relies on when it suggests
 * "exclude unsupported architectures in your Xcode project's Build Settings by setting
 * EXCLUDED_ARCHS" — without this filter, Xcode's `ARCHS` env var leaks excluded archs into
 * Gradle's run-script phase invocation (KT-86142).
 */
class XcodeEnvironmentTest {

    @Test
    fun `EXCLUDED_ARCHS removes archs from targets`() {
        val env = newEnvironment(
            sdk = "iphonesimulator",
            archs = "arm64 x86_64",
            excludedArchs = "x86_64",
        )
        assertEquals(listOf(KonanTarget.IOS_SIMULATOR_ARM64), env.targets)
    }

    @Test
    fun `EXCLUDED_ARCHS that strips all archs yields empty targets`() {
        val env = newEnvironment(
            sdk = "iphonesimulator",
            archs = "x86_64",
            excludedArchs = "x86_64",
        )
        assertEquals(emptyList(), env.targets)
    }

    @Test
    fun `EXCLUDED_ARCHS unset behaves like the old code path`() {
        val env = newEnvironment(
            sdk = "iphonesimulator",
            archs = "arm64 x86_64",
            excludedArchs = null,
        )
        @Suppress("DEPRECATION") // KT-81704: apple x64 family deprecation
        assertEquals(setOf(KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.IOS_X64), env.targets.toSet())
    }

    @Test
    fun `EXCLUDED_ARCHS ignores blank entries`() {
        val env = newEnvironment(
            sdk = "iphonesimulator",
            archs = "arm64 x86_64",
            excludedArchs = "  x86_64   ",
        )
        assertEquals(listOf(KonanTarget.IOS_SIMULATOR_ARM64), env.targets)
    }

    @Test
    fun `archs returns empty list when ARCHS env var is unset`() {
        val env = newEnvironment(sdk = "iphonesimulator", archs = null)
        assertEquals(emptyList(), env.archs)
        assertEquals(emptyList(), env.targets)
    }

    private fun newEnvironment(
        sdk: String?,
        archs: String?,
        excludedArchs: String? = null,
    ): XcodeEnvironment {
        val project = buildProject()
        val prefix = XcodeEnvironment.XCODE_ENVIRONMENT_OVERRIDE_KEY
        project.setOrClearExtra("$prefix.SDK_NAME", sdk)
        project.setOrClearExtra("$prefix.ARCHS", archs)
        project.setOrClearExtra("$prefix.EXCLUDED_ARCHS", excludedArchs)
        return XcodeEnvironment(project)
    }

    private fun Project.setOrClearExtra(name: String, value: String?) {
        extensions.extraProperties.set(name, value)
    }
}

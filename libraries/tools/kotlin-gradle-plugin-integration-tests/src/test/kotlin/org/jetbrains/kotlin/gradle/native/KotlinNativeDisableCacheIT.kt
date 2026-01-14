/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.net.URI

@DisplayName("Kotlin/Native disable cache tests")
@NativeGradlePluginTests
internal class KotlinNativeDisableCacheIT : KGPBaseTest() {

    @OptIn(KotlinNativeCacheApi::class)
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC, OS.LINUX], enabledOnCI = [OS.LINUX])
    fun testNativeCacheDisabledDiagnostic(
        gradleVersion: GradleVersion,
    ) {
        val isMac = HostManager.hostIsMac
        setupNativeCacheTest(
            gradleVersion,
            targetProvider = { if (isMac) macosArm64() else linuxX64() }
        ) {
            val taskName = getDebugStaticTaskName(if (isMac) "macosArm64" else "linuxX64")

            build(taskName) {
                assertHasDiagnostic(KotlinToolingDiagnostics.NativeCacheDisabledDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NativeCacheRedundantDiagnostic)
            }
        }
    }

    @OptIn(KotlinNativeCacheApi::class)
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC, OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.WINDOWS])
    fun testNativeCacheRedundantDiagnostic(
        gradleVersion: GradleVersion,
    ) {
        setupNativeCacheTest(
            gradleVersion,
            targetProvider = { mingwX64() }
        ) {
            val taskName = getDebugStaticTaskName("mingwX64")

            // Caching is not supported for this target on this host, making the disable call redundant.
            build(taskName) {
                assertNoDiagnostic(KotlinToolingDiagnostics.NativeCacheDisabledDiagnostic)
                assertHasDiagnostic(KotlinToolingDiagnostics.NativeCacheRedundantDiagnostic)
            }
        }
    }
}

internal class KotlinNativeDisableCacheUnsupportedHostIT : KGPDaemonsBaseTest() {

    /**
     * Defines the parameters for a Linux Arm64 host environment.
     * These parameters are used to specify the operating system name and architecture.
     */
    private val linuxArm64HostParameters = listOf("-Dos.name=Linux", "-Dos.arch=aarch64")

    @OptIn(KotlinNativeCacheApi::class)
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC, OS.LINUX], enabledOnCI = [OS.LINUX])
    fun testNoNativeCacheDiagnosticsOnUnsupportedHost(
        gradleVersion: GradleVersion,
    ) {
        val isMac = HostManager.hostIsMac
        setupNativeCacheTest(
            gradleVersion,
            targetProvider = { if (isMac) macosArm64() else linuxX64() }
        ) {
            val taskName = getDebugStaticTaskName(if (isMac) "macosArm64" else "linuxX64")

            // Simulate a host (Linux Arm64) that cannot build the defined target.
            // result: The link task is NOT registered.
            val args = listOf(taskName) + linuxArm64HostParameters

            build(*args.toTypedArray()) {
                // Confirm that the host mismatch was detected
                assertHasDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)

                // Confirm that link task is skipped
                assertTasksSkipped(taskName)

                // Since diagnostics are emitted during Task Execution, and the task
                // was never found/executed, we expect NO diagnostics to be present.
                assertNoDiagnostic(KotlinToolingDiagnostics.NativeCacheDisabledDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NativeCacheRedundantDiagnostic)
            }
        }
    }
}

private fun getDebugStaticTaskName(targetName: String) = lowerCamelCaseName(
    ":linkDebugStatic",
    targetName
)

@OptIn(KotlinNativeCacheApi::class)
private fun KGPBaseTest.setupNativeCacheTest(
    gradleVersion: GradleVersion,
    targetProvider: KotlinMultiplatformExtension.() -> KotlinNativeTarget,
    testBlock: TestProject.() -> Unit
) {
    project("empty", gradleVersion) {
        plugins {
            kotlin("multiplatform")
        }
        buildScriptInjection {
            project.applyMultiplatform {
                val target = targetProvider()

                target.binaries.staticLib {
                    @Suppress("DEPRECATION")
                    disableNativeCache(
                        DisableCacheInKotlinVersion.`2_3_20`,
                        "Disabled for integration testing",
                        URI("https://kotlinlang.org")
                    )
                }
                sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
            }
        }
        testBlock()
    }
}
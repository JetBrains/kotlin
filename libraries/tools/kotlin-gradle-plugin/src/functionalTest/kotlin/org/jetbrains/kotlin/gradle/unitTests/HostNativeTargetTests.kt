/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost
import org.jetbrains.kotlin.gradle.dsl.konanTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.withModifiedSystemProperties
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@Isolated("Modifies system properties")
class HostNativeTargetTests {

    private data class SimulatedHost(val osName: String, val osArch: String, val konanTarget: KonanTarget, val presetName: String)

    private val supportedHosts = listOf(
        SimulatedHost("Mac OS X", "aarch64", KonanTarget.MACOS_ARM64, "macosArm64"),
        SimulatedHost("Linux", "amd64", KonanTarget.LINUX_X64, "linuxX64"),
        SimulatedHost("Windows 11", "amd64", KonanTarget.MINGW_X64, "mingwX64"),
    )

    @Test
    fun `hostNativeTarget declares the host target under its preset name`() {
        supportedHosts.forEach { host ->
            withModifiedSystemProperties("os.name" to host.osName, "os.arch" to host.osArch) {
                with(buildProjectWithMPP()) {
                    var configured = false
                    val target = multiplatformExtension.hostNativeTarget { configured = true }
                    assertNotNull(target, "target on ${host.osName}/${host.osArch}")
                    assertEquals(host.presetName, target.name)
                    assertEquals(host.konanTarget, target.konanTarget)
                    assertTrue(configured, "configure block must run")
                    evaluate()
                    assertNoDiagnostics(KotlinToolingDiagnostics.HostNativeTargetUnavailable)
                }
            }
        }
    }

    @Test
    fun `hostNativeTarget with a custom name uses that name`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                val target = multiplatformExtension.hostNativeTarget("native")
                assertNotNull(target)
                assertEquals("native", target.name)
                assertEquals(KonanTarget.LINUX_X64, target.konanTarget)
                assertSame(target, multiplatformExtension.targets.getByName("native"))
            }
        }
    }

    @Test
    fun `hostNativeTarget returns the same target when called twice`() {
        withModifiedSystemProperties("os.name" to "Mac OS X", "os.arch" to "aarch64") {
            with(buildProjectWithMPP()) {
                val first = multiplatformExtension.hostNativeTarget()
                val second = multiplatformExtension.hostNativeTarget()
                assertSame(first, second)
                assertSame(first, multiplatformExtension.macosArm64())
                assertEquals(1, multiplatformExtension.targets.filterIsInstance<KotlinNativeTarget>().size)
            }
        }
    }

    @Test
    fun `hostNativeTarget on an unsupported host declares nothing and warns once`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "aarch64") {
            with(buildProjectWithMPP()) {
                multiplatformExtension.jvm()
                assertNull(multiplatformExtension.hostNativeTarget())
                assertNull(multiplatformExtension.hostNativeTarget("native"))
                assertTrue(multiplatformExtension.targets.filterIsInstance<KotlinNativeTarget>().isEmpty())
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.HostNativeTargetUnavailable)
                assertNoDiagnostics(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `canonical name does not clash with the default hierarchy template`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                multiplatformExtension.jvm()
                multiplatformExtension.hostNativeTarget()
                evaluate()
                assertNoDiagnostics(KotlinToolingDiagnostics.KotlinDefaultHierarchyFallbackIllegalTargetNames)
            }
        }
    }

    @Test
    fun `native as a name clashes with the default hierarchy template`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                multiplatformExtension.jvm()
                multiplatformExtension.hostNativeTarget("native")
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.KotlinDefaultHierarchyFallbackIllegalTargetNames)
            }
        }
    }

    @Test
    fun `nativeHost is the current host`() {
        withModifiedSystemProperties("os.name" to "Windows 11", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                assertEquals(KotlinNativeHost.current(), multiplatformExtension.nativeHost)
                assertTrue(multiplatformExtension.nativeHost.isWindows)
                assertEquals(KonanTarget.MINGW_X64, multiplatformExtension.nativeHost.konanTarget)
            }
        }
    }
}

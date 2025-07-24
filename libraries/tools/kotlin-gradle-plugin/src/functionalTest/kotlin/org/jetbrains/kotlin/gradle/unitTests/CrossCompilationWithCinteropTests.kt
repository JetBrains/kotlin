/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_ENABLE_KLIBS_CROSSCOMPILATION
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.tcs
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CrossCompilationWithCinteropTests {

    @Test
    fun `cross compilation with cinterops`() {
        val project = buildProjectWithMPP {
            kotlin {
                listOf(macosArm64(), linuxX64(), mingwX64()).forEach { target ->
                    target.binaries.executable(listOf(NativeBuildType.DEBUG))
                }

                addDummyCinterop { it.konanTarget == KonanTarget.MACOS_ARM64 }
                addDummyCinterop { it.konanTarget == KonanTarget.LINUX_X64 }
                addDummyCinterop { it.konanTarget == KonanTarget.MINGW_X64 }
            }
        }.evaluate()

        val compileKotlinMacosArm64 = project.tasks.findByName("compileKotlinMacosArm64") as KotlinNativeCompile?
        val compileKotlinMingwX64 = project.tasks.findByName("compileKotlinMingwX64") as KotlinNativeCompile?
        val compileKotlinLinuxX64 = project.tasks.findByName("compileKotlinLinuxX64") as KotlinNativeCompile?

        val cinteropDummyMacosArm64 = project.tasks.findByName("cinteropDummyMacosArm64") as CInteropProcess?
        val cinteropDummyLinuxX64 = project.tasks.findByName("cinteropDummyLinuxX64") as CInteropProcess?
        val cinteropDummyMingwX64 = project.tasks.findByName("cinteropDummyMingwX64") as CInteropProcess?

        assertNotNull(compileKotlinMingwX64, "compileKotlinMingwX64 task should be present")
        assertNotNull(compileKotlinLinuxX64, "compileKotlinLinuxX64 task should be present")
        assertNotNull(compileKotlinMacosArm64, "compileKotlinMacosArm64 task should be present")

        assertNotNull(cinteropDummyMingwX64, "cinteropDummyMingwX64 task should be present")
        assertNotNull(cinteropDummyLinuxX64, "cinteropDummyLinuxX64 task should be present")
        assertNotNull(cinteropDummyMacosArm64, "cinteropDummyMacosArm64 task should be present")

        val linuxX64Compilation = compileKotlinLinuxX64.compilation.tcs.compilation as KotlinNativeCompilation
        val mingwX64Compilation = compileKotlinMingwX64.compilation.tcs.compilation as KotlinNativeCompilation
        val macosArm64Compilation = compileKotlinMacosArm64.compilation.tcs.compilation as KotlinNativeCompilation

        assert(linuxX64Compilation.crossCompilationSupported.get()) {
            "LinuxX64 compilation should support cross-compilation"
        }

        assert(mingwX64Compilation.crossCompilationSupported.get()) {
            "MingwX64 compilation should support cross-compilation"
        }

        if (HostManager.hostIsMac) {
            project.assertNoDiagnostics(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
            assert(cinteropDummyMacosArm64.enabled) {
                "cinteropDummyMacosArm64 task should be enabled on macOS"
            }
            assert(macosArm64Compilation.crossCompilationSupported.get()) {
                "MacosArm64 compilation should support cross-compilation on macOS"
            }
            assertEquals(
                true,
                compileKotlinMacosArm64.onlyIf.isSatisfiedBy(compileKotlinMacosArm64),
                "compileKotlinMacosArm64 task should be enabled on macOS"
            )
        } else {
            project.assertContainsDiagnostic(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
            assert(!cinteropDummyMacosArm64.enabled) {
                "cinteropDummyMacosArm64 task should be disabled on non-macOS"
            }
            assert(!macosArm64Compilation.crossCompilationSupported.get()) {
                "MacosArm64 compilation should not support cross-compilation on non-macOS"
            }
            assertEquals(
                false,
                compileKotlinMacosArm64.onlyIf.isSatisfiedBy(compileKotlinMacosArm64),
                "compileKotlinMacosArm64 task should be disabled on non-macOS"
            )
        }
    }

    @Test
    fun `cross compilation disabled without cinterops`() {
        val project = buildProject {
            propertiesExtension.set(KOTLIN_NATIVE_ENABLE_KLIBS_CROSSCOMPILATION, "false")
            applyMultiplatformPlugin()
            kotlin {
                macosArm64()
                linuxX64()
                mingwX64()
            }
        }.evaluate()

        val compileKotlinMacosArm64 = project.tasks.findByName("compileKotlinMacosArm64") as? KotlinNativeCompile
        val compileKotlinMingwX64 = project.tasks.findByName("compileKotlinMingwX64") as? KotlinNativeCompile
        val compileKotlinLinuxX64 = project.tasks.findByName("compileKotlinLinuxX64") as? KotlinNativeCompile

        assertNotNull(compileKotlinMingwX64, "compileKotlinMingwX64 task should be present")
        assertNotNull(compileKotlinLinuxX64, "compileKotlinLinuxX64 task should be present")
        assertNotNull(compileKotlinMacosArm64, "compileKotlinMacosArm64 task should be present")

        project.assertNoDiagnostics(KotlinToolingDiagnostics.CrossCompilationWithCinterops)

        if (HostManager.hostIsMac) {
            assertEquals(
                true,
                compileKotlinMacosArm64.onlyIf.isSatisfiedBy(compileKotlinMacosArm64),
                "compileKotlinMacosArm64 task should be enabled on macOS"
            )
        } else {
            assertEquals(
                false,
                compileKotlinMacosArm64.onlyIf.isSatisfiedBy(compileKotlinMacosArm64),
                "compileKotlinMacosArm64 task should be disabled on non-macOS"
            )
        }
    }
}

private fun KotlinMultiplatformExtension.addDummyCinterop(spec: (KotlinNativeTarget) -> Boolean) {
    targets
        .withType(KotlinNativeTarget::class.java)
        .matching(spec)
        .configureEach { target ->
            target.compilations
                .getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                .cinterops
                .create("dummy") {
                    it.defFile("dummy.def")
                }
        }
}


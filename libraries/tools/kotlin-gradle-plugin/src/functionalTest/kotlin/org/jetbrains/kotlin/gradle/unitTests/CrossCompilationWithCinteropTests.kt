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
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
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
                @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
                macosX64()
                linuxX64()
                mingwX64()

                addDummyCinterop { it.konanTarget == KonanTarget.MACOS_X64 }
                addDummyCinterop { it.konanTarget == KonanTarget.LINUX_X64 }
                addDummyCinterop { it.konanTarget == KonanTarget.MINGW_X64 }
            }
        }.evaluate()

        val compileKotlinMacosX64 = project.tasks.findByName("compileKotlinMacosX64") as? KotlinNativeCompile
        val compileKotlinMingwX64 = project.tasks.findByName("compileKotlinMingwX64") as? KotlinNativeCompile
        val compileKotlinLinuxX64 = project.tasks.findByName("compileKotlinLinuxX64") as? KotlinNativeCompile

        val cinteropDummyMacosX64 = project.tasks.findByName("cinteropDummyMacosX64")
        val cinteropDummyLinuxX64 = project.tasks.findByName("cinteropDummyLinuxX64")
        val cinteropDummyMingwX64 = project.tasks.findByName("cinteropDummyMingwX64")

        assertNotNull(compileKotlinMingwX64, "compileKotlinMingwX64 task should be present")
        assertNotNull(compileKotlinLinuxX64, "compileKotlinLinuxX64 task should be present")
        assertNotNull(compileKotlinMacosX64, "compileKotlinMacosX64 task should be present")

        assertNotNull(cinteropDummyMingwX64, "cinteropDummyMingwX64 task should be present")
        assertNotNull(cinteropDummyLinuxX64, "cinteropDummyLinuxX64 task should be present")
        assertNotNull(cinteropDummyMacosX64, "cinteropDummyMacosX64 task should be present")

        if (HostManager.hostIsMac) {
            project.assertNoDiagnostics(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
            assert(cinteropDummyMacosX64.enabled) {
                "cinteropDummyMacosX64 task should be enabled on macOS"
            }
            assertEquals(
                true,
                compileKotlinMacosX64.onlyIf.isSatisfiedBy(compileKotlinMacosX64),
                "compileKotlinMacosX64 task should be enabled on macOS"
            )
        } else {
            project.assertContainsDiagnostic(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
            assert(!cinteropDummyMacosX64.enabled) {
                "cinteropDummyMacosX64 task should be disabled on non-macOS"
            }
            assertEquals(
                false,
                compileKotlinMacosX64.onlyIf.isSatisfiedBy(compileKotlinMacosX64),
                "compileKotlinMacosX64 task should be disabled on non-macOS"
            )
        }
    }

    @Test
    fun `cross compilation disabled without cinterops`() {
        val project = buildProject {
            propertiesExtension.set(KOTLIN_NATIVE_ENABLE_KLIBS_CROSSCOMPILATION, "false")
            applyMultiplatformPlugin()
            kotlin {
                @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
                macosX64()
                linuxX64()
                mingwX64()
            }
        }.evaluate()

        val compileKotlinMacosX64 = project.tasks.findByName("compileKotlinMacosX64") as? KotlinNativeCompile
        val compileKotlinMingwX64 = project.tasks.findByName("compileKotlinMingwX64") as? KotlinNativeCompile
        val compileKotlinLinuxX64 = project.tasks.findByName("compileKotlinLinuxX64") as? KotlinNativeCompile

        assertNotNull(compileKotlinMingwX64, "compileKotlinMingwX64 task should be present")
        assertNotNull(compileKotlinLinuxX64, "compileKotlinLinuxX64 task should be present")
        assertNotNull(compileKotlinMacosX64, "compileKotlinMacosX64 task should be present")

        project.assertNoDiagnostics(KotlinToolingDiagnostics.CrossCompilationWithCinterops)

        if (HostManager.hostIsMac) {
            assertEquals(
                true,
                compileKotlinMacosX64.onlyIf.isSatisfiedBy(compileKotlinMacosX64),
                "compileKotlinMacosX64 task should be enabled on macOS"
            )
        } else {
            assertEquals(
                false,
                compileKotlinMacosX64.onlyIf.isSatisfiedBy(compileKotlinMacosX64),
                "compileKotlinMacosX64 task should be disabled on non-macOS"
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


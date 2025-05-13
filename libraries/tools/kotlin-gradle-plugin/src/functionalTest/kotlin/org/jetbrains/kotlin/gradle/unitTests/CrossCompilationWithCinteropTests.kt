/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_DISABLE_KLIBS_CROSSCOMPILATION
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Test
import kotlin.test.assertNotNull

class CrossCompilationWithCinteropTests {

    @Test
    fun `cross compilation with cinterops`() {
        val project = buildProjectWithMPP {
            kotlin {
                macosX64()
                linuxX64()
                mingwX64()

                addDummyCinterop { it.konanTarget == KonanTarget.MACOS_X64 }
                addDummyCinterop { it.konanTarget == KonanTarget.LINUX_X64 }
                addDummyCinterop { it.konanTarget == KonanTarget.MINGW_X64 }
            }
        }.evaluate()

        val compileKotlinMacosX64 = project.tasks.findByName("compileKotlinMacosX64")
        val compileKotlinMingwX64 = project.tasks.findByName("compileKotlinMingwX64")
        val compileKotlinLinuxX64 = project.tasks.findByName("compileKotlinLinuxX64")

        val cinteropDummyMacosX64 = project.tasks.findByName("cinteropDummyMacosX64")
        val cinteropDummyLinuxX64 = project.tasks.findByName("cinteropDummyLinuxX64")
        val cinteropDummyMingwX64 = project.tasks.findByName("cinteropDummyMingwX64")

        assertNotNull(compileKotlinMingwX64, "compileKotlinMingwX64 task should be present")
        assertNotNull(compileKotlinLinuxX64, "compileKotlinLinuxX64 task should be present")
        assertNotNull(compileKotlinMacosX64, "compileKotlinMacosX64 task should be present")

        assertNotNull(cinteropDummyMingwX64, "cinteropDummyMingwX64 task should be present")
        assertNotNull(cinteropDummyLinuxX64, "cinteropDummyLinuxX64 task should be present")
        assertNotNull(cinteropDummyMacosX64, "cinteropDummyMacosX64 task should be present")

        assert(compileKotlinMacosX64.enabled) {
            "compileKotlinMacosX64 task should be enabled"
        }

        if (HostManager.hostIsMac) {
            assert(cinteropDummyMacosX64.enabled) {
                "cinteropDummyMacosX64 task should be enabled on macOS"
            }
        } else {
            assert(!cinteropDummyMacosX64.enabled) {
                "cinteropDummyMacosX64 task should be disabled on non-macOS"
            }
        }
    }

    @Test
    fun `cross compilation disabled without cinterops`() {
        val project = buildProject {
            propertiesExtension.set(KOTLIN_NATIVE_DISABLE_KLIBS_CROSSCOMPILATION, "true")
            applyMultiplatformPlugin()
            kotlin {
                macosX64()
                linuxX64()
                mingwX64()
            }
        }.evaluate()

        val compileKotlinMacosX64 = project.tasks.findByName("compileKotlinMacosX64")
        val compileKotlinMingwX64 = project.tasks.findByName("compileKotlinMingwX64")
        val compileKotlinLinuxX64 = project.tasks.findByName("compileKotlinLinuxX64")

        assertNotNull(compileKotlinMingwX64, "compileKotlinMingwX64 task should be present")
        assertNotNull(compileKotlinLinuxX64, "compileKotlinLinuxX64 task should be present")
        assertNotNull(compileKotlinMacosX64, "compileKotlinMacosX64 task should be present")

        if (HostManager.hostIsMac) {
            assert(compileKotlinMacosX64.enabled) {
                "compileKotlinMacosX64 task should be enabled on macOS"
            }
        } else {
            assert(!compileKotlinMacosX64.enabled) {
                "compileKotlinMacosX64 task should be disabled on non-macOS"
            }
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


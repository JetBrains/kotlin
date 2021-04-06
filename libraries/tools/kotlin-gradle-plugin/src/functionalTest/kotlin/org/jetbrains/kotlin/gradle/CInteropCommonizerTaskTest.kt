/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/* Associate compilations are not yet supported by the IDE. KT-34102 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizationParameters
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerTask
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizeCInteropTask
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import kotlin.test.*

class CInteropCommonizerTaskTest {

    private lateinit var project: ProjectInternal
    private lateinit var kotlin: KotlinMultiplatformExtension
    private val task: CInteropCommonizerTask get() = project.commonizeCInteropTask?.get() ?: fail("Missing commonizeCInteropTask")

    @BeforeTest
    fun setup() {
        project = ProjectBuilder.builder().build() as ProjectInternal
        project.extensions.getByType(ExtraPropertiesExtension::class.java).set("kotlin.mpp.enableGranularSourceSetsMetadata", "true")
        project.extensions.getByType(ExtraPropertiesExtension::class.java).set("kotlin.mpp.enableCInteropCommonization", "true")
        project.plugins.apply("kotlin-multiplatform")
        kotlin = project.extensions.getByName("kotlin") as KotlinMultiplatformExtension
    }


    @Test
    fun `nativeMain linux macos`() {
        val linuxInterop = kotlin.linuxX64("linux").compilations.getByName("main").cinterops.create("anyInteropName")
        val macosInterop = kotlin.macosX64("macos").compilations.getByName("main").cinterops.create("anyInteropName")

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val nativeMain = kotlin.sourceSets.create("nativeMain")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val macosMain = kotlin.sourceSets.getByName("macosMain")

        nativeMain.dependsOn(commonMain)
        linuxMain.dependsOn(nativeMain)
        macosMain.dependsOn(nativeMain)

        project.evaluate()
        val nativeMainCompilation = kotlin.targets.flatMap { it.compilations }
            .filterIsInstance<KotlinSharedNativeCompilation>()
            .single { it.defaultSourceSet == nativeMain }

        assertEquals(
            CInteropCommonizationParameters(
                CommonizerTarget(LINUX_X64, MACOS_X64), setOf(linuxInterop.identifier, macosInterop.identifier)
            ),
            task.getCommonizationParameters(nativeMainCompilation)
        )
    }

    @Test
    fun `nativeMain linux macos (no macos interop defined)`() {
        kotlin.linuxX64("linux").compilations.getByName("main").cinterops.create("anyInteropName")
        kotlin.macosX64("macos")

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val nativeMain = kotlin.sourceSets.create("nativeMain")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val macosMain = kotlin.sourceSets.getByName("macosMain")

        nativeMain.dependsOn(commonMain)
        linuxMain.dependsOn(nativeMain)
        macosMain.dependsOn(nativeMain)

        project.evaluate()
        val nativeMainCompilation = kotlin.targets.flatMap { it.compilations }
            .filterIsInstance<KotlinSharedNativeCompilation>()
            .single { it.defaultSourceSet == nativeMain }

        assertNull(
            task.getCommonizationParameters(nativeMainCompilation),
            "Expected no CInteropCommonizerTarget from nativeMain, since one target has not defined any cinterop"
        )
    }


    @Test
    fun `nativeMain iosMain linux macos iosX64 iosArm64`() {
        val linuxInterop = kotlin.linuxX64("linux").compilations.getByName("main").cinterops.create("anyInteropName").identifier
        val macosInterop = kotlin.macosX64("macos").compilations.getByName("main").cinterops.create("anyInteropName").identifier
        val iosX64Interop = kotlin.iosX64("iosX64").compilations.getByName("main").cinterops.create("anyInteropName").identifier
        val iosArm64Interop = kotlin.iosArm64("iosArm64").compilations.getByName("main").cinterops.create("anyInteropName").identifier

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val nativeMain = kotlin.sourceSets.create("nativeMain")
        val iosMain = kotlin.sourceSets.create("iosMain")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val macosMain = kotlin.sourceSets.getByName("macosMain")
        val iosX64Main = kotlin.sourceSets.getByName("iosX64Main")
        val iosArm64Main = kotlin.sourceSets.getByName("iosArm64Main")

        nativeMain.dependsOn(commonMain)
        linuxMain.dependsOn(nativeMain)
        macosMain.dependsOn(nativeMain)
        iosMain.dependsOn(nativeMain)
        iosX64Main.dependsOn(iosMain)
        iosArm64Main.dependsOn(iosMain)

        project.evaluate()

        assertEquals(
            CInteropCommonizationParameters(
                SharedCommonizerTarget(CommonizerTarget(IOS_X64, IOS_ARM64), CommonizerTarget(MACOS_X64), CommonizerTarget(LINUX_X64)),
                setOf(linuxInterop, macosInterop, iosX64Interop, iosArm64Interop)
            ), task.getCommonizationParameters(sharedNativeCompilation(nativeMain))
        )

        assertEquals(
            CInteropCommonizationParameters(
                SharedCommonizerTarget(CommonizerTarget(IOS_X64, IOS_ARM64), CommonizerTarget(MACOS_X64), CommonizerTarget(LINUX_X64)),
                setOf(linuxInterop, macosInterop, iosX64Interop, iosArm64Interop)
            ), task.getCommonizationParameters(sharedNativeCompilation(iosMain))
        )

        assertTrue(
            task.getCommonizationParameters(sharedNativeCompilation(iosMain))!! in
                    task.getCommonizationParameters(sharedNativeCompilation(nativeMain))!!,
            "Expected CInteropCommonizerTarget of iosMain to be fully contained in nativeMain"
        )
    }

    private fun sharedNativeCompilation(sourceSet: KotlinSourceSet): KotlinSharedNativeCompilation {
        return kotlin.targets.flatMap { it.compilations }.filterIsInstance<KotlinSharedNativeCompilation>()
            .single { it.defaultSourceSet == sourceSet }
    }
}

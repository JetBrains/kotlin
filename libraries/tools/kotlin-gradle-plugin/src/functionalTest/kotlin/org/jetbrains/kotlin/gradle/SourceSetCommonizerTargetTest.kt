/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/* Associate compilations are not yet supported by the IDE. KT-34102 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.native.internal.getCommonizerTarget
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SourceSetCommonizerTargetTest {

    private lateinit var project: Project
    private lateinit var kotlin: KotlinMultiplatformExtension

    @BeforeTest
    fun setup() {
        project = ProjectBuilder.builder().build()
        addBuildEventsListenerRegistryMock(project)
        project.extensions.getByType(ExtraPropertiesExtension::class.java).set("kotlin.mpp.enableCompatibilityMetadataVariant", "false")
        project.plugins.apply("kotlin-multiplatform")
        kotlin = project.extensions.getByName("kotlin") as KotlinMultiplatformExtension

    }

    @Test
    fun `linux macos`() {
        kotlin.linuxX64("linux")
        kotlin.macosX64("macos")

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val macosMain = kotlin.sourceSets.getByName("macosMain")
        val linuxTest = kotlin.sourceSets.getByName("linuxTest")
        val macosTest = kotlin.sourceSets.getByName("macosTest")

        assertEquals(CommonizerTarget(LINUX_X64), project.getCommonizerTarget(linuxMain))
        assertEquals(CommonizerTarget(LINUX_X64), project.getCommonizerTarget(linuxTest))
        assertEquals(CommonizerTarget(MACOS_X64), project.getCommonizerTarget(macosMain))
        assertEquals(CommonizerTarget(MACOS_X64), project.getCommonizerTarget(macosTest))
        assertEquals(CommonizerTarget(LINUX_X64, MACOS_X64), project.getCommonizerTarget(commonMain))
        assertEquals(CommonizerTarget(LINUX_X64, MACOS_X64), project.getCommonizerTarget(commonTest))
    }

    @Test
    fun `nativeMain linux macos`() {
        kotlin.linuxX64("linux")
        kotlin.macosX64("macos")

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val nativeMain = kotlin.sourceSets.create("nativeMain")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val macosMain = kotlin.sourceSets.getByName("macosMain")
        val linuxTest = kotlin.sourceSets.getByName("linuxTest")
        val macosTest = kotlin.sourceSets.getByName("macosTest")

        nativeMain.dependsOn(commonMain)
        linuxMain.dependsOn(nativeMain)
        macosMain.dependsOn(nativeMain)

        assertEquals(CommonizerTarget(LINUX_X64), project.getCommonizerTarget(linuxMain))
        assertEquals(CommonizerTarget(LINUX_X64), project.getCommonizerTarget(linuxTest))
        assertEquals(CommonizerTarget(MACOS_X64), project.getCommonizerTarget(macosMain))
        assertEquals(CommonizerTarget(MACOS_X64), project.getCommonizerTarget(macosTest))

        assertEquals(CommonizerTarget(LINUX_X64, MACOS_X64), project.getCommonizerTarget(nativeMain))
        assertEquals(CommonizerTarget(LINUX_X64, MACOS_X64), project.getCommonizerTarget(commonMain))
        assertEquals(CommonizerTarget(LINUX_X64, MACOS_X64), project.getCommonizerTarget(commonTest))
    }

    @Test
    fun `nativeMain linuxX64-a linuxX64-b`() {
        kotlin.linuxX64("linuxA")
        kotlin.linuxX64("linuxB")

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val nativeMain = kotlin.sourceSets.create("nativeMain")
        val linuxAMain = kotlin.sourceSets.getByName("linuxAMain")
        val linuxBMain = kotlin.sourceSets.getByName("linuxBMain")
        val linuxATest = kotlin.sourceSets.getByName("linuxATest")
        val linuxBTest = kotlin.sourceSets.getByName("linuxBTest")

        nativeMain.dependsOn(commonMain)
        linuxAMain.dependsOn(nativeMain)
        linuxBMain.dependsOn(nativeMain)

        assertEquals(LeafCommonizerTarget(LINUX_X64), project.getCommonizerTarget(linuxAMain))
        assertEquals(LeafCommonizerTarget(LINUX_X64), project.getCommonizerTarget(linuxATest))
        assertEquals(LeafCommonizerTarget(LINUX_X64), project.getCommonizerTarget(linuxBMain))
        assertEquals(LeafCommonizerTarget(LINUX_X64), project.getCommonizerTarget(linuxBTest))

        assertEquals(LeafCommonizerTarget(LINUX_X64), project.getCommonizerTarget(nativeMain))
        assertEquals(LeafCommonizerTarget(LINUX_X64), project.getCommonizerTarget(commonMain))
        assertEquals(LeafCommonizerTarget(LINUX_X64), project.getCommonizerTarget(commonTest))
    }

    @Test
    fun `nativeMain iosMain linux macos iosX64 iosArm64`() {
        kotlin.linuxX64("linux")
        kotlin.macosX64("macos")
        kotlin.iosX64("iosX64")
        kotlin.iosArm64("iosArm64")

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val nativeMain = kotlin.sourceSets.create("nativeMain")
        val iosMain = kotlin.sourceSets.create("iosMain")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val macosMain = kotlin.sourceSets.getByName("macosMain")
        val linuxTest = kotlin.sourceSets.getByName("linuxTest")
        val macosTest = kotlin.sourceSets.getByName("macosTest")
        val iosX64Main = kotlin.sourceSets.getByName("iosX64Main")
        val iosX64Test = kotlin.sourceSets.getByName("iosX64Test")
        val iosArm64Main = kotlin.sourceSets.getByName("iosArm64Main")
        val iosArm64Test = kotlin.sourceSets.getByName("iosArm64Test")

        nativeMain.dependsOn(commonMain)
        linuxMain.dependsOn(nativeMain)
        macosMain.dependsOn(nativeMain)
        iosMain.dependsOn(nativeMain)
        iosX64Main.dependsOn(iosMain)
        iosArm64Main.dependsOn(iosMain)

        assertEquals(CommonizerTarget(LINUX_X64), project.getCommonizerTarget(linuxMain))
        assertEquals(CommonizerTarget(LINUX_X64), project.getCommonizerTarget(linuxTest))
        assertEquals(CommonizerTarget(MACOS_X64), project.getCommonizerTarget(macosMain))
        assertEquals(CommonizerTarget(MACOS_X64), project.getCommonizerTarget(macosTest))
        assertEquals(CommonizerTarget(IOS_X64), project.getCommonizerTarget(iosX64Test))
        assertEquals(CommonizerTarget(IOS_X64), project.getCommonizerTarget(iosX64Test))
        assertEquals(CommonizerTarget(IOS_ARM64), project.getCommonizerTarget(iosArm64Main))
        assertEquals(CommonizerTarget(IOS_ARM64), project.getCommonizerTarget(iosArm64Test))
        assertEquals(CommonizerTarget(IOS_X64, IOS_ARM64), project.getCommonizerTarget(iosMain))

        assertEquals(
            CommonizerTarget(IOS_X64, IOS_ARM64, MACOS_X64, LINUX_X64),
            project.getCommonizerTarget(nativeMain)
        )
        assertEquals(
            CommonizerTarget(IOS_X64, IOS_ARM64, MACOS_X64, LINUX_X64),
            project.getCommonizerTarget(commonMain)
        )

        assertEquals(
            CommonizerTarget(IOS_ARM64, IOS_X64, LINUX_X64, MACOS_X64),
            project.getCommonizerTarget(commonTest)
        )
    }

    @Test
    fun `nativeMain linux macos jvm`() {
        kotlin.linuxX64("linux")
        kotlin.macosX64("macos")
        kotlin.jvm("jvm")

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val nativeMain = kotlin.sourceSets.create("nativeMain")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val macosMain = kotlin.sourceSets.getByName("macosMain")
        val linuxTest = kotlin.sourceSets.getByName("linuxTest")
        val macosTest = kotlin.sourceSets.getByName("macosTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")

        nativeMain.dependsOn(commonMain)
        linuxMain.dependsOn(nativeMain)
        macosMain.dependsOn(nativeMain)

        assertEquals(CommonizerTarget(LINUX_X64), project.getCommonizerTarget(linuxMain))
        assertEquals(CommonizerTarget(LINUX_X64), project.getCommonizerTarget(linuxTest))
        assertEquals(CommonizerTarget(MACOS_X64), project.getCommonizerTarget(macosMain))
        assertEquals(CommonizerTarget(MACOS_X64), project.getCommonizerTarget(macosTest))
        assertNull(project.getCommonizerTarget(jvmMain), "Expected jvmMain to have no commonizer target")
        assertNull(project.getCommonizerTarget(jvmTest), "Expected jvmTest to have no commonizer target")

        assertEquals(CommonizerTarget(LINUX_X64, MACOS_X64), project.getCommonizerTarget(nativeMain))

        assertNull(project.getCommonizerTarget(commonMain), "Expected commonMain to have no commonizer target")
        assertNull(project.getCommonizerTarget(commonTest), "Expected commonTest to have no commonizer target")
    }

    @Test
    fun `nativeMain with non hmpp workaround`() {
        val linux1 = kotlin.linuxX64("linux1")
        val linux2 = kotlin.linuxArm64("linux2")

        val nativeMain = kotlin.sourceSets.create("nativeMain")

        listOf(linux1, linux2).forEach { target ->
            target.compilations.getByName("main").source(nativeMain)
        }

        assertEquals(
            SharedCommonizerTarget(setOf(linux1.konanTarget, linux2.konanTarget)),
            project.getCommonizerTarget(nativeMain)
        )
    }

    @Test
    fun `orphan source sets are ignored`() {
        val linux1 = kotlin.linuxX64("linux1")
        val linux2 = kotlin.linuxArm64("linux2")
        val nativeMain = kotlin.sourceSets.create("nativeMain")
        val linux1Main = kotlin.sourceSets.getByName("linux1Main")
        val linux2Main = kotlin.sourceSets.getByName("linux2Main")
        val orphan = kotlin.sourceSets.create("orphan")

        linux1Main.dependsOn(nativeMain)
        linux2Main.dependsOn(nativeMain)
        orphan.dependsOn(nativeMain)

        assertEquals(CommonizerTarget(linux1.konanTarget, linux2.konanTarget), project.getCommonizerTarget(nativeMain))
    }

    @Test
    fun `orphan source sets only`() {
        val nativeMain = kotlin.sourceSets.create("nativeMain")
        val orphan1 = kotlin.sourceSets.create("orphan1")
        val orphan2 = kotlin.sourceSets.create("orphan2")

        orphan1.dependsOn(nativeMain)
        orphan2.dependsOn(nativeMain)

        assertEquals(null, project.getCommonizerTarget(nativeMain))
    }
}

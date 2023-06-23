/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.iosMain
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.iosTest
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import kotlin.test.Test

class MppSourceSetConventionsDiagnosticTests {

    @Test
    fun `test - jvmMain and jvmTest without jvm target`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            linuxX64()
            sourceSets.jvmMain
            sourceSets.jvmTest

            configurationResult.await()
            checkDiagnostics("PlatformSourceSetConventionUsedWithoutCorrespondingTarget-jvmMain-jvmTest")
        }
    }

    @Test
    fun `test - jvmMain and jvmTest with custom named jvm target`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            jvm("desktop")
            sourceSets.jvmMain
            sourceSets.jvmTest

            configurationResult.await()
            checkDiagnostics("PlatformSourceSetConventionUsedWithCustomTargetName-jvmMain-jvmTest")
        }
    }

    @Test
    fun `test - jsMain and jsTest without js target`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            linuxX64()
            sourceSets.jsMain
            sourceSets.jsTest

            configurationResult.await()
            checkDiagnostics("PlatformSourceSetConventionUsedWithoutCorrespondingTarget-jsMain-jsTest")
        }
    }

    @Test
    fun `test - jsMain and jsTest with custom named js target`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            js("custom") { nodejs() }
            sourceSets.jsMain
            sourceSets.jsTest

            configurationResult.await()
            checkDiagnostics("PlatformSourceSetConventionUsedWithCustomTargetName-jsMain-jsTest")
        }
    }

    @Test
    fun `test - jvmMain and jvmTest without jvm target - source sets created manually`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            linuxX64()
            sourceSets.create("jvmMain")
            sourceSets.create("jvmTest")

            configurationResult.await()
            checkDiagnostics("PlatformSourceSetConventionUsedWithoutCorrespondingTarget-manually-created-jvmMain-jvmTest")
        }
    }

    @Test
    fun `test - androidMain - without androidTarget`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            linuxX64()
            sourceSets.androidMain

            configurationResult.await()
            checkDiagnostics("AndroidMainSourceSetConventionUsedWithoutAndroidTarget")
        }
    }

    @Test
    fun `test - iosMain and iosTest - without any ios target`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.linuxX64()
        multiplatformExtension.jvm()

        multiplatformExtension.sourceSets.iosMain
        multiplatformExtension.sourceSets.iosTest

        configurationResult.await()
        checkDiagnostics("IosSourceSetConventionUsedWithoutIosTarget")
    }
}

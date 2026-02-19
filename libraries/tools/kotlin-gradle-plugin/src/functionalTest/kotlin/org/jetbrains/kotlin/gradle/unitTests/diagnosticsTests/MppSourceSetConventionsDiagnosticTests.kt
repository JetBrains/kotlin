/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.iosMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.iosTest
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import kotlin.test.Test
import org.jetbrains.kotlin.gradle.util.checkDiagnostics as checkDiagnosticsUtil

class MppSourceSetConventionsDiagnosticTests {
    private fun Project.checkDiagnostics(name: String) = checkDiagnosticsUtil("MppSourceSetConventionsDiagnosticTests/$name")

    @Test
    fun `test - jvmMain and jvmTest without jvm target`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            linuxX64()
            sourceSets.jvmMain
            sourceSets.jvmTest

            configurationResult.await()
            checkDiagnostics("jvmMain-jvmTest-without-jvm")
        }
    }

    @Test
    fun `test - jvmMain and jvmTest with custom named jvm target`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            jvm("desktop")
            sourceSets.jvmMain
            sourceSets.jvmTest

            configurationResult.await()
            checkDiagnostics("jvmMain-jvmTest-with-custom-jvm")
        }
    }

    @Test
    fun `test - jsMain and jsTest without js target`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            linuxX64()
            sourceSets.jsMain
            sourceSets.jsTest

            configurationResult.await()
            checkDiagnostics("jsMain-jsTest-without-js")
        }
    }

    @Test
    fun `test - jsMain and jsTest with custom named js target`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            js("custom") { nodejs() }
            sourceSets.jsMain
            sourceSets.jsTest

            configurationResult.await()
            checkDiagnostics("jsMain-jsTest-with-custom-js")
        }
    }

    @Test
    fun `test - jvmMain and jvmTest without jvm target - source sets created manually`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            linuxX64()
            sourceSets.create("jvmMain")
            sourceSets.create("jvmTest")

            configurationResult.await()
            checkDiagnostics("manually-created-jvmMain-jvmTest")
        }
    }

    @Test
    fun `test - androidMain - without androidTarget`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            linuxX64()
            sourceSets.androidMain

            configurationResult.await()
            checkDiagnostics("androidMain-without-android")
        }
    }

    @Test
    fun `test - linuxX64Main and linuxX64Test - without linuxX64 target`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            jvm()
            sourceSets.linuxX64Main
            sourceSets.linuxX64Test

            configurationResult.await()
            checkDiagnostics("linuxX64Main-linuxX64Test-without-linuxX64")
        }
    }

    @Test
    fun `test - linuxX64Main and linuxX64Test - with custom linuxX64 target`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            linuxX64("custom")
            sourceSets.linuxX64Main
            sourceSets.linuxX64Test

            configurationResult.await()
            checkDiagnostics("linuxX64Main-linuxX64Test-with-custom-linuxX64")
        }
    }

    @Test
    fun `test - linuxArm64 and linuxX64 names swapped with actual targets`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            linuxX64("linuxArm64")
            linuxArm64("linuxX64")

            sourceSets.linuxX64Main
            sourceSets.linuxX64Test
            sourceSets.linuxArm64Main
            sourceSets.linuxArm64Test

            configurationResult.await()
            checkDiagnostics("linuxX64-linuxArm64-swapped")
        }
    }

    @Test
    fun `test - iosMain and iosTest - without any ios target`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.linuxX64()
        multiplatformExtension.jvm()

        multiplatformExtension.sourceSets.iosMain
        multiplatformExtension.sourceSets.iosTest

        configurationResult.await()
        checkDiagnostics("iosMain-without-ios")
    }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.enableCInteropCommonization
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import kotlin.test.*

class DisabledCInteropCommonizationWarningTest {

    private val project by lazy { ProjectBuilder.builder().build() as ProjectInternal }

    @Test
    fun `test warning shows affected source sets, affected cinterops and how to hide the warning`() {
        project.enableCInteropCommonization(false)
        project.setupNativeTargetsWithCInterops()
        project.evaluate()

        val warningMessage = project.getWarningMessage()

        assertNotNull(warningMessage, "Expected a warning message to be logged")

        assertTrue(
            "[commonMain, nativeMain]" in warningMessage,
            "Expected source sets being mentioned in the warning message. Found\n\n$warningMessage"
        )

        assertTrue(
            "[" +
                    "cinterop:compilation/compileKotlinLinuxArm64/dummy1, " +
                    "cinterop:compilation/compileKotlinLinuxArm64/dummy2, " +
                    "cinterop:compilation/compileKotlinLinuxX64/dummy1, " +
                    "cinterop:compilation/compileKotlinLinuxX64/dummy2" +
                    "]" in warningMessage,
            "Expected affected cinterops being mentioned in the warning message. Found\n\n$warningMessage"
        )

        assertTrue(
            "$KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION.nowarn=true" in warningMessage,
            "Expected warning message to include hint on how to disable the warning"
        )
    }

    @Test
    fun `test warning is not shown when explicitly ignored`() {
        project.enableCInteropCommonization(false)
        project.setupNativeTargetsWithCInterops()
        project.propertiesExtension.set("$KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION.nowarn", "true")
        project.evaluate()

        assertNull(
            project.getWarningMessage(),
            "Expected no warning message shown when explicitly ignored"
        )
    }

    @Test
    fun `test warning is not shown when cinterop commonization is enabled`() {
        project.enableCInteropCommonization(true)
        project.setupNativeTargetsWithCInterops()
        project.evaluate()
        assertNull(
            project.getWarningMessage(),
            "Expected no error message shown when cinterop commonization is enabled"
        )
    }

    @Test
    fun `test warning is not shown when no cinterops are defined`() {
        project.enableCInteropCommonization(false)
        project.setupNativeTargets()
        project.evaluate()
        assertNull(
            project.getWarningMessage(),
            "Expected no error message shown when no cinterops are defined"
        )
    }
}

private fun Project.setupNativeTargets(): List<KotlinNativeTarget> {
    val kotlin = applyMultiplatformPlugin()
    val linuxX64 = kotlin.linuxX64()
    val linuxArm64 = kotlin.linuxArm64()

    val commonMain = kotlin.sourceSets.getByName("commonMain")
    val nativeMain = kotlin.sourceSets.create("nativeMain")
    val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
    val linuxArm64Main = kotlin.sourceSets.getByName("linuxArm64Main")

    nativeMain.dependsOn(commonMain)
    linuxX64Main.dependsOn(nativeMain)
    linuxArm64Main.dependsOn(nativeMain)

    return listOf(linuxX64, linuxArm64)
}

private fun Project.setupNativeTargetsWithCInterops(): List<KotlinNativeTarget> {
    return setupNativeTargets().onEach { target ->
        target.compilations.getByName("main").cinterops.create("dummy1")
        target.compilations.getByName("main").cinterops.create("dummy2")
    }
}

private fun Project.getWarningMessage(): String? {
    val diagnostics = project.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this).filter {
        it.id == KotlinToolingDiagnostics.DisabledCinteropsCommonizationInHmppProject.id
    }

    if (diagnostics.size > 1) fail("Unexpected multiple diagnostics reported:\n\n" + diagnostics.joinToString(separator = "\n\n"))

    return diagnostics.singleOrNull()?.message
}

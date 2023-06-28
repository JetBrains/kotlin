/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.jvmMain
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import kotlin.test.Test

class DefaultHierarchySetupDiagnosticTest {
    @Test
    fun `test - warning KotlinTargetHierarchyFallbackDependsOnUsageDetected`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = multiplatformExtension
        kotlin.jvm()

        /* Create intermediate SourceSet using custom .dependsOn calls */
        val intermediateMain = kotlin.sourceSets.create("intermediateMain")
        intermediateMain.dependsOn(kotlin.sourceSets.commonMain.get())
        kotlin.sourceSets.jvmMain.get().dependsOn(intermediateMain)

        project.launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
            project.checkDiagnostics("KotlinDefaultHierarchyFallbackDependsOnUsageDetected")
        }
    }

    @Test
    fun `test - warning KotlinTargetHierarchyFallbackIllegalTargetNames`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = multiplatformExtension
        kotlin.linuxX64("linux") // <- illegal: Will clash with 'linux' group
        kotlin.linuxArm64("Native") // <-- illegal: Will clash with 'native' group
        kotlin.jvm()

        project.launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
            project.checkDiagnostics("KotlinDefaultHierarchyFallbackIllegalTargetNames")
        }
    }

    @Test
    fun `test - warning - ios shortcut`() {
        Assume.assumeTrue(HostManager.hostIsMac)

        buildProjectWithMPP().runLifecycleAwareTest {
            val kotlin = multiplatformExtension
            @Suppress("DEPRECATION")
            kotlin.ios()

            project.launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
                project.checkDiagnostics("KotlinDefaultHierarchyFallbackNativeTargetShortcutUsageDetected-ios")
            }
        }
    }

    @Test
    fun `test - warning - watchos shortcut`() {
        Assume.assumeTrue(HostManager.hostIsMac)

        buildProjectWithMPP().runLifecycleAwareTest {
            val kotlin = multiplatformExtension
            @Suppress("DEPRECATION")
            kotlin.watchos()

            project.launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
                project.checkDiagnostics("KotlinDefaultHierarchyFallbackNativeTargetShortcutUsageDetected-watchos")
            }
        }
    }

    @Test
    fun `test - warning - tvos shortcut`() {
        Assume.assumeTrue(HostManager.hostIsMac)

        buildProjectWithMPP().runLifecycleAwareTest {
            val kotlin = multiplatformExtension
            @Suppress("DEPRECATION")
            kotlin.tvos()

            project.launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
                project.checkDiagnostics("KotlinDefaultHierarchyFallbackNativeTargetShortcutUsageDetected-tvos")
            }
        }
    }
}

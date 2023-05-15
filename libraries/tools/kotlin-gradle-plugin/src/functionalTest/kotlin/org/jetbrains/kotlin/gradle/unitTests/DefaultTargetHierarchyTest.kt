/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_DEFAULT_TARGET_HIERARCHY
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy.defaultKotlinTargetHierarchy
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class DefaultTargetHierarchyTest {
    @Test
    fun `test - default target hierarchy is applied for simple project`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = multiplatformExtension
        kotlin.jvm()
        kotlin.linuxX64()
        kotlin.linuxArm64()

        launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
            assertEquals(
                listOf(defaultKotlinTargetHierarchy), kotlin.internalKotlinTargetHierarchy.appliedDescriptors,
                "Expected 'defaultKotlinTargetHierarchy' to be applied"
            )
        }
    }

    @Test
    fun `test - default target hierarchy is disabled by flag`() = buildProjectWithMPP().runLifecycleAwareTest {
        project.extraProperties.set(KOTLIN_MPP_ENABLE_DEFAULT_TARGET_HIERARCHY, false.toString())

        val kotlin = multiplatformExtension
        kotlin.jvm()
        kotlin.linuxX64()
        kotlin.linuxArm64()

        launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
            if (kotlin.internalKotlinTargetHierarchy.appliedDescriptors.isNotEmpty()) {
                fail("Expected *no* target hierarchy descriptor to be applied")
            }

            project.assertNoDiagnostics()
        }
    }

    @Test
    fun `test - no hierarchy descriptor applied - when a custom dependsOn edge was configured`() =
        buildProjectWithMPP().runLifecycleAwareTest {
            val kotlin = multiplatformExtension
            kotlin.jvm()
            kotlin.sourceSets.jvmMain.get().dependsOn(kotlin.sourceSets.commonMain.get())

            launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
                if (kotlin.internalKotlinTargetHierarchy.appliedDescriptors.isNotEmpty()) {
                    fail("Expected *no* target hierarchy descriptor to be applied")
                }
            }
        }

    @Test
    fun `test - no hierarchy descriptor applied - when a targetName clashes with a default group`() {
        buildProjectWithMPP().runLifecycleAwareTest {
            val kotlin = multiplatformExtension
            kotlin.linuxX64("linux") // <- group in default hierarchy is also called 'linux'

            launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
                if (kotlin.internalKotlinTargetHierarchy.appliedDescriptors.isNotEmpty()) {
                    fail("Expected *no* target hierarchy descriptor to be applied")
                }
            }
        }
    }
}

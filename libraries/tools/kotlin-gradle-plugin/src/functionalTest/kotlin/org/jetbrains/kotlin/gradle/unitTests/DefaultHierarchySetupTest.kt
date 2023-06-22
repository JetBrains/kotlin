/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.jvmMain
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class DefaultHierarchySetupTest {
    @Test
    fun `test - default target hierarchy is applied for simple project`() = buildProjectWithMPP().runLifecycleAwareTest {
        val kotlin = multiplatformExtension
        kotlin.jvm()
        kotlin.linuxX64()
        kotlin.linuxArm64()

        launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
            assertEquals(
                setOf(KotlinHierarchyTemplate.default), kotlin.hierarchy.appliedTemplates,
                "Expected 'defaultKotlinTargetHierarchy' to be applied"
            )
        }
    }

    @Test
    fun `test - default target hierarchy is disabled by flag`() = buildProjectWithMPP().runLifecycleAwareTest {
        project.extraProperties.set(KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE, false.toString())

        val kotlin = multiplatformExtension
        kotlin.jvm()
        kotlin.linuxX64()
        kotlin.linuxArm64()

        launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
            if (kotlin.hierarchy.appliedTemplates.isNotEmpty()) {
                fail("Expected *no* hierarchy template to be applied")
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
                if (kotlin.hierarchy.appliedTemplates.isNotEmpty()) {
                    fail("Expected *no* hierarchy template to be applied")
                }
            }
        }

    @Test
    fun `test - no hierarchy descriptor applied - when a targetName clashes with a default group`() {
        buildProjectWithMPP().runLifecycleAwareTest {
            val kotlin = multiplatformExtension
            kotlin.linuxX64("linux") // <- group in default hierarchy is also called 'linux'

            launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
                if (kotlin.hierarchy.appliedTemplates.isNotEmpty()) {
                    fail("Expected *no* hierarchy template to be applied")
                }
            }
        }
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript
import org.jetbrains.kotlin.gradle.plugin.currentKotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.util.allCauses
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.gradle.utils.isPluginApplied
import kotlin.test.*

class PluginManagerUtilTest {

    private fun buildSampleProject(action: Project.() -> Unit = {}) = buildProjectWithMPP {
        action()

        kotlin {
            jvm()
            linuxX64()
        }
    }

    @Test
    fun `test - isPluginApplied - when plugin applied`() {
        val project = buildSampleProject {
            plugins.apply("maven-publish")
        }
        project.runLifecycleAwareTest {
            assertTrue(isPluginApplied("maven-publish"))
        }
    }

    @Test
    fun `test - isPluginApplied - call withPluginId before plugin is applied`() {
        val project = buildSampleProject {}
        project.runLifecycleAwareTest {
            launch { assertTrue(isPluginApplied("maven-publish")) }
            project.plugins.apply("maven-publish")
        }
    }

    @Test
    fun `test - withPluginId - plugin not applied`() {
        val project = buildSampleProject()

        project.runLifecycleAwareTest {
            assertFalse(isPluginApplied("maven-publish"))
            val currentStage = currentKotlinPluginLifecycle().stage
            assertTrue(currentStage <= AfterEvaluateBuildscript)
        }
    }

    @Test
    fun `test - withPluginId - plugin can't be applied after build script evaluation`() {
        val project = buildSampleProject()

        val error = assertFails {
            project.runLifecycleAwareTest {
                launchInStage(AfterEvaluateBuildscript.nextOrLast) { plugins.apply("maven-publish") }
                isPluginApplied("maven-publish")
            }
        }

        val errorMessages = error.allCauses.map { it.message }
        val expectedErrorMessage = "Plugin 'maven-publish' was applied too late. It was expected to be applied during build script evaluation"
        if (expectedErrorMessage !in errorMessages) fail("Expected to fail with: $expectedErrorMessage")
    }
}
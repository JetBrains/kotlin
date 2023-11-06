/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript
import org.jetbrains.kotlin.gradle.plugin.currentKotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.gradle.utils.withPluginId
import kotlin.test.Test
import kotlin.test.assertTrue

class PluginManagerUtilTest {

    private fun buildSampleProject(action: Project.() -> Unit = {}) = buildProjectWithMPP {
        action()

        kotlin {
            jvm()
            linuxX64()
        }
    }

    @Test
    fun `test - withPluginId - when plugin applied`() {
        val project = buildProject {
            plugins.apply("maven-publish")
        }
        val future = project.withPluginId(
            pluginId = "maven-publish",
            whenApplied = { true },
            whenNotApplied = { false }
        )

        assertTrue(future.getOrThrow())
    }

    @Test
    fun `test - withPluginId - call withPluginId before plugin is applied`() {
        val project = buildProject {}
        val future = project.withPluginId(
            pluginId = "maven-publish",
            whenApplied = { true },
            whenNotApplied = { false }
        )
        project.plugins.apply("maven-publish")

        assertTrue(future.getOrThrow())
    }

    @Test
    fun `test - withPluginId - plugin not applied`() {
        val project = buildSampleProject()

        val future = project.withPluginId(
            pluginId = "some-plugin-id",
            whenApplied = { false },
            whenNotApplied = { true },
        )

        project.runLifecycleAwareTest {
            assertTrue(future.await())
            val currentStage = currentKotlinPluginLifecycle().stage
            assertTrue(currentStage <= AfterEvaluateBuildscript)
        }
    }
}
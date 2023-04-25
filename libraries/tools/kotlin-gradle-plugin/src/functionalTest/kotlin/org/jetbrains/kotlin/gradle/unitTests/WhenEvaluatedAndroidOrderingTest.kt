/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WhenEvaluatedAndroidOrderingTest {

    private lateinit var project: ProjectInternal

    @BeforeTest
    fun setup() {
        project = ProjectBuilder.builder().build() as ProjectInternal
    }

    /**
     * Check that the `whenEvaluated` actions that are scheduled before the Android plugin is applied get triggered only after the actions
     * done in the Android plugin's afterEvaluate phase
     */
    @Test
    fun `test Android compilations visible in whenEvaluated`() {
        project.applyGradleBuiltInPlugins()

        val kotlin = project.applyMultiplatformPlugin()

        var triggered = false

        project.whenEvaluated {
            /** These are created by the Kotlin plugin immediately on Android plugin's afterEvaluate actions */
            val androidCompilations = kotlin.targets.getByName("android").compilations
            assertTrue { androidCompilations.isNotEmpty() }

            assertFalse(triggered, "whenEvaluated should call the function only once")
            triggered = true
        }

        project.applyAndroidLibraryPlugin()
        kotlin.androidTarget()

        project.evaluate()

        assertTrue { triggered }
    }

    // Apply these built-in plugins, so that Gradle doesn't apply them in `project.evaluate()`
    // below and trigger the plugin application callbacks after Android is applied
    private fun Project.applyGradleBuiltInPlugins() {
        plugins.apply(org.gradle.api.plugins.HelpTasksPlugin::class.java)
        plugins.apply(org.gradle.buildinit.plugins.BuildInitPlugin::class.java)
        plugins.apply(org.gradle.buildinit.plugins.WrapperPlugin::class.java)
    }

    private fun Project.applyAndroidLibraryPlugin() {
        project.plugins.apply("android-library")
        val android = project.extensions.getByName("android") as LibraryExtension
        android.compileSdk = 31
    }
}
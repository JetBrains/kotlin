/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import com.android.builder.errors.EvalIssueException
import org.gradle.api.Project
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradleSubplugin
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.test.MuteableTestRule
import org.junit.Rule
import kotlin.test.Test

class ComposePluginSuggestApplyCheckerTest {
    @get:Rule val muteableTestRule = MuteableTestRule()

    private val isAgp850AndAbove get() = AndroidGradlePluginVersion.current >= AndroidGradlePluginVersion(8, 5, 0)

    @Test
    fun shouldSuggestToApplyComposePluginInAndroidProject() {
        val project = buildProjectWithKotlinAndroidPlugin()

        if (isAgp850AndAbove) {
            val exception = assertFailsWithChainedCause<EvalIssueException> { project.evaluate() }
            assertContains("Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required", exception.message)
        } else {
            project.evaluate()
            project.checkDiagnostics("ComposePluginSuggestApplyChecker")
        }
    }

    @Test
    fun shouldNotSuggestToApplyComposePluginInAndroidProject() {
        val project = buildProjectWithKotlinAndroidPlugin(
            preApplyCode = {
                project.plugins.apply(ComposeCompilerGradleSubplugin::class.java)
            }
        )

        project.evaluate()

        project.assertNoDiagnostics()
    }

    @Test
    fun shouldSuggestToApplyComposePluginInMppAndroidProject() {
        val project = buildProjectWithMppPluginAndAndroid()

        project.evaluate()

        project.checkDiagnostics("ComposePluginSuggestApplyChecker")
    }

    @Test
    fun shouldNotSuggestToApplyComposePluginInMppAndroidProject() {
        val project = buildProjectWithMppPluginAndAndroid(
            preApplyCode = {
                project.plugins.apply(ComposeCompilerGradleSubplugin::class.java)
            }
        )

        project.evaluate()

        project.assertNoDiagnostics()
    }

    private fun buildProjectWithKotlinAndroidPlugin(
        preApplyCode: Project.() -> Unit = {}
    ) = buildProject({}) {
        preApplyCode()
        applyKotlinAndroidPlugin()

        androidApplication {
            compileSdk = 33
            buildFeatures {
                compose = true
            }
        }
    }

    private fun buildProjectWithMppPluginAndAndroid(
        preApplyCode: Project.() -> Unit = {}
    ) = buildProject({}) {
        preApplyCode()

        androidApplication {
            compileSdk = 33
            buildFeatures {
                compose = true
            }
        }

        applyMultiplatformPlugin()

        multiplatformExtension.androidTarget()
    }
}
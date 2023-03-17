/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.junit.Test
import kotlin.test.Ignore
import kotlin.test.assertTrue

class KpmCompilerPluginMppIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    @Ignore
    fun testTransientPluginOptions() = with(transformProjectWithPluginsDsl("kpmTransientPluginOptions")) {
        val currentGradleVersion = chooseWrapperVersionOrFinishTest()
        val options = defaultBuildOptions().suppressDeprecationWarningsSinceGradleVersion(
            TestVersions.Gradle.G_7_4,
            currentGradleVersion,
            "Workaround for KT-55751"
        )

        fun updatePluginOptions(regularOptionValue: String, transientOptionValue: String) {
            gradleProperties().writeText(
                """
                    test-plugin.regular=$regularOptionValue
                    test-plugin.transient=$transientOptionValue
                    """.trimIndent()
            )
        }

        updatePluginOptions("XXX", "YYY")
        build("compileKotlin", options = options) {
            assertSuccessful()
            assertTasksExecuted(":compileKotlinJvm")
            compilerArgs(":compileKotlinJvm").also { args ->
                assertTrue(
                    args.contains("plugin:test-plugin:regular=XXX"),
                    "Expected regular plugin option in compilation args"
                )
                assertTrue(
                    args.contains("plugin:test-plugin:transient=YYY"),
                    "Expected transient plugin option in compilation args"
                )
            }
        }

        // When transient plugin option change
        updatePluginOptions("XXX", "ZZZ")
        build("compileKotlin", options = options) {
            assertSuccessful()
            assertTasksUpToDate(":compileKotlinJvm")
        }

        // When regular plugin option change
        updatePluginOptions("ZZZ", "ZZZ")
        build("compileKotlin", options = options) {
            assertSuccessful()
            assertTasksExecuted(":compileKotlinJvm")
            compilerArgs(":compileKotlinJvm").also { args ->
                assertTrue(
                    args.contains("plugin:test-plugin:regular=ZZZ"),
                    "Expected regular plugin option in compilation args"
                )
                assertTrue(
                    args.contains("plugin:test-plugin:transient=ZZZ"),
                    "Expected transient plugin option in compilation args"
                )
            }
        }
    }
}

/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.junit.Test
import kotlin.test.assertTrue

class KpmCompilerPluginMppIT : BaseGradleIT() {
    override fun defaultBuildOptions() = super.defaultBuildOptions().copy(
        // Workaround for KT-55751
        warningMode = WarningMode.None,
    )

    @Test
    fun testTransientPluginOptions() {
        val project = transformProjectWithPluginsDsl("kpmTransientPluginOptions")
        fun updatePluginOptions(regularOptionValue: String, transientOptionValue: String) {
            project.gradleProperties().writeText(
                """
                    test-plugin.regular=$regularOptionValue
                    test-plugin.transient=$transientOptionValue
                    """.trimIndent()
            )
        }

        updatePluginOptions("XXX", "YYY")
        project.build("compileKotlin") {
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
        project.build("compileKotlin") {
            assertSuccessful()
            assertTasksUpToDate(":compileKotlinJvm")
        }

        // When regular plugin option change
        updatePluginOptions("ZZZ", "ZZZ")
        project.build("compileKotlin") {
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

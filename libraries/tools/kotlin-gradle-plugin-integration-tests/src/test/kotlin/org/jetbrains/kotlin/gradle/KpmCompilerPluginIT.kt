/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.junit.Test
import kotlin.test.assertTrue

class KpmCompilerPluginIT : BaseGradleIT() {

    @Test
    fun testSensitivePluginOptions() {
        val project = transformProjectWithPluginsDsl("kpmSensitivePluginOptions")
        fun updatePluginOptions(sensitiveValue: String, insensitiveValue: String) {
            project.gradleProperties().writeText(
                """
                    test-plugin.sensitive=$sensitiveValue
                    test-plugin.insensitive=$insensitiveValue
                    """.trimIndent()
            )
        }

        updatePluginOptions("XXX", "YYY")
        project.build("compileKotlin") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlinJvm")
            compilerArgs(":compileKotlinJvm").also { args ->
                assertTrue(
                    args.contains("plugin:test-plugin:sensitive=XXX"),
                    "Expected sensitive plugin option in compilation args"
                )
                assertTrue(
                    args.contains("plugin:test-plugin:insensitive=YYY"),
                    "Expected insensitive plugin option in compilation args"
                )
            }
        }

        // When insensitive plugin option change
        updatePluginOptions("XXX", "ZZZ")
        project.build("compileKotlin") {
            assertSuccessful()
            assertTasksUpToDate(":compileKotlinJvm")
        }

        // When sensitive plugin option change
        updatePluginOptions("ZZZ", "ZZZ")
        project.build("compileKotlin") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlinJvm")
            compilerArgs(":compileKotlinJvm").also { args ->
                assertTrue(
                    args.contains("plugin:test-plugin:sensitive=ZZZ"),
                    "Expected sensitive plugin option in compilation args"
                )
                assertTrue(
                    args.contains("plugin:test-plugin:insensitive=ZZZ"),
                    "Expected insensitive plugin option in compilation args"
                )
            }
        }
    }
}

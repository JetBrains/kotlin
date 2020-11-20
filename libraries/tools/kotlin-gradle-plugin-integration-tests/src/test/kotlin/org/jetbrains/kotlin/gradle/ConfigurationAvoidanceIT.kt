/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.junit.Test

class ConfigurationAvoidanceIT : BaseGradleIT() {

    @Test
    fun testUnrelatedTaskNotConfigured() = with(Project("simpleProject")) {
        setupWorkingDir()

        val expensivelyConfiguredTaskName = "expensivelyConfiguredTask"
        val triggeredExpensiveConfigurationText = "Triggered expensive configuration!"

        gradleBuildScript().appendText("\n" + """
            tasks.register("$expensivelyConfiguredTaskName") {
                println("$triggeredExpensiveConfigurationText")
            }
        """.trimIndent())

        build("compileKotlin") {
            assertSuccessful()
            assertNotContains(triggeredExpensiveConfigurationText)
        }
    }

    @Test
    fun testAndroidUnrelatedTaskNotConfigured() = with(
        Project(
            "AndroidProject",
            gradleVersionRequirement = GradleVersionRequired.AtLeast("6.6.1")
        )
    ) {
        setupWorkingDir()

        listOf("Android", "Test").forEach { subproject ->
            gradleBuildScript(subproject).appendText("\n" + """
                android {
                    applicationVariants.all {
                        it.getAidlCompileProvider().configure {
                            throw new RuntimeException("Task should not be configured.")
                        }
                    }
                }
                """.trimIndent()
            )
        }

        gradleBuildScript("Lib").appendText(
            "\n" + """
            android {
                libraryVariants.all {
                    it.getAidlCompileProvider().configure {
                        throw new RuntimeException("Task should not be configured.")
                    }
                }
            }
        """.trimIndent()
        )

        build(
            "help", options = defaultBuildOptions().copy(
                androidGradlePluginVersion = AGPVersion.v4_2_0
            )
        ) {
            assertSuccessful()
        }
    }
}
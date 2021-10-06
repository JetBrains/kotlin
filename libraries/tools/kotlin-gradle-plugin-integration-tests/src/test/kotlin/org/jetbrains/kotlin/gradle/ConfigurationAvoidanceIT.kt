/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.appendText

@DisplayName("Tasks configuration avoidance")
@SimpleGradlePluginTests
@OptIn(ExperimentalPathApi::class)
class ConfigurationAvoidanceIT : KGPBaseTest() {

    @DisplayName("Unrelated tasks are not configured")
    @GradleTest
    fun testUnrelatedTaskNotConfigured(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {

            val expensivelyConfiguredTaskName = "expensivelyConfiguredTask"

            @Suppress("GroovyAssignabilityCheck")
            rootBuildGradle.appendText(
                //language=Groovy
                """
                    
                tasks.register("$expensivelyConfiguredTaskName") {
                    throw new GradleException("Should not configure expensive task!")
                }
                """.trimIndent()
            )

            build("compileKotlin")
        }
    }

    @DisplayName("Android tasks are not configured")
    @GradleTestVersions(minVersion = "6.7.1")
    @GradleTest
    fun testAndroidUnrelatedTaskNotConfigured(gradleVersion: GradleVersion) {
        project(
            "AndroidProject",
            gradleVersion
        ) {

            listOf("Android", "Test").forEach { subproject ->
                projectPath
                    .resolve(subproject)
                    .resolve("build.gradle")
                    .appendText(
                        //language=Groovy
                        """
                        
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

            projectPath
                .resolve("Lib")
                .resolve("build.gradle")
                .appendText(
                    //language=Groovy
                    """
                    
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
                "help",
                buildOptions = defaultBuildOptions.copy(
                    androidVersion = TestVersions.AGP.AGP_42
                )
            )
        }
    }
}
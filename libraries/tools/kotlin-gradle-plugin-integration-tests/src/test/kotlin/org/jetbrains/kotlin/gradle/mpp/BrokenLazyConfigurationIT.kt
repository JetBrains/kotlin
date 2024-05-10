/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.readText

@DisplayName("Broken task configuration avoidance doesn't lead to build failures at least with simple setups")
class BrokenLazyConfigurationIT : KGPBaseTest() {
    @JvmGradlePluginTests
    @GradleTest
    @DisplayName("works in JVM")
    fun testBrokenTcaInJvm(gradleVersion: GradleVersion) {
        project("kotlinJavaProject", gradleVersion) {
            assert("sourceSets {" in buildGradle.readText())
            buildGradle.modify {
                it.replace(
                    "sourceSets {",
                    """
                        tasks.whenTaskAdded {} // break lazy initialization of all tasks
                        sourceSets {
                    """.trimIndent()
                )
            }
            build("build")
        }
    }

    @MppGradlePluginTests
    @GradleTest
    @DisplayName("works in JS")
    fun testBrokenTcaInJs(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            val subprojects = listOf("app", "base", "lib")
            for (subproject in subprojects) {
                val subprojectBuildScript = subProject(subproject).buildGradleKts
                assert("dependencies {" in subprojectBuildScript.readText())
                subprojectBuildScript.modify {
                    it.replace(
                        "dependencies {",
                        """
                        tasks.whenTaskAdded {} // break lazy initialization of all tasks
                        dependencies {
                    """.trimIndent()
                    )
                }
            }
            build("build")
        }
    }

    @MppGradlePluginTests
    @GradleTest
    @DisplayName("works in MPP") // aka KT-56131
    fun testBrokenTcaInMpp(gradleVersion: GradleVersion) {
        project("new-mpp-lib-with-tests", gradleVersion) {
            assert("apply plugin: 'kotlin-multiplatform'" in buildGradle.readText())
            buildGradle.modify {
                it.replace(
                    "apply plugin: 'kotlin-multiplatform'",
                    """
                        tasks.whenTaskAdded {} // break lazy initialization of all tasks
                        apply plugin: 'kotlin-multiplatform'
                    """.trimIndent()
                )
            }
            build("build")
        }
    }

    @MppGradlePluginTests
    @GradleTest
    // Required Gradle APIs to avoid buildDir early capture only available since 7.4
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_4)
    @DisplayName("Changing build directory after task configuration doesn't lead to failures")
    fun changingBuildDirInMpp(gradleVersion: GradleVersion) {
        project("new-mpp-lib-with-tests", gradleVersion) {
            buildGradle.appendText(
                """
                    tasks.whenTaskAdded {} // break lazy initialization of all tasks
                    
                    project.layout.buildDirectory.set(project.layout.projectDirectory.dir("build2"))
                """.trimIndent()
            )
            build("build") {
                assertDirectoryInProjectDoesNotExist("build")

                assertDirectoryInProjectExists("build2")
            }
        }
    }
}

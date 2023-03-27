/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("Broken task configuration avoidance doesn't lead to build failures at least with simple setups")
class BrokenLazyConfigurationIT : MPPBaseTest() {
    @GradleTest
    @DisplayName("works in JVM")
    fun testBrokenTcaInJvm(gradleVersion: GradleVersion) {
        project("kotlinJavaProject", gradleVersion) {
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

    @GradleTest
    @DisplayName("works in JS")
    fun testBrokenTcaInJs(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            val subprojects = listOf("app", "base", "lib")
            for (subproject in subprojects) {
                subProject(subproject).buildGradleKts.modify {
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

    @GradleTest
    @DisplayName("works in MPP") // aka KT-56131
    fun testBrokenTcaInMpp(gradleVersion: GradleVersion) {
        project("new-mpp-lib-with-tests", gradleVersion) {
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
}
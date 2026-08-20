/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("Tests for empty MPP test source sets")
class MppEmptyTestSourceSetsIT : KGPBaseTest() {

    @DisplayName("No test sources in commonTest or jvmTest")
    @GradleTest
    fun testNoTestSources(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            buildScriptInjection {
                kotlinMultiplatform.jvm()
            }
            kotlinSourcesDir("commonMain").source("CommonMain.kt") {
                """
                package org.example.project

                fun commonFun(): String = "common"
                """.trimIndent()
            }

            build(":jvmTest") {
                assertTasksNoSource(":compileTestKotlinJvm", ":jvmTest")
                assertNoTestResultsProduced("jvmTest")
            }
        }
    }

    // In Gradle 9.0+, executing a test task fails by default when test compilation outputs exist but 0 tests are discovered.
    // Older Gradle versions succeeded silently.
    @DisplayName("Sources in jvmTest without test cases fail on Gradle 9.0+")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_9_0)
    @GradleTest
    fun testSourcesWithoutTests(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            buildScriptInjection {
                kotlinMultiplatform.jvm()
            }
            kotlinSourcesDir("jvmTest").source("NotATest.kt") {
                """
                package org.example.project

                class NotATest {
                    fun helper(): String = "helper"
                }
                """.trimIndent()
            }

            buildAndFail(":jvmTest") {
                assertTasksExecuted(":compileTestKotlinJvm")
                assertTasksFailed(":jvmTest")
                assertNoTestResultsProduced("jvmTest")
            }
        }
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@MppGradlePluginTests
@DisplayName("Tests for Multiplatform test reporting")
class MppTestReportIT : KGPBaseTest() {

    @DisplayName("Aggregated test report contains results from multiple targets (:allTests)")
    @GradleTest
    fun testAllTestsReportAggregation(gradleVersion: GradleVersion) {
        project(
            "base-kotlin-multiplatform-library",
            gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            buildScriptInjection {
                kotlinMultiplatform.jvm()
                kotlinMultiplatform.js {
                    nodejs()
                }
                kotlinMultiplatform.sourceSets.getByName("commonTest").dependencies {
                    implementation(kotlin("test"))
                }
            }

            kotlinSourcesDir("commonTest").source("org/example/project/SampleTest.kt") {
                """
                package org.example.project

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class SampleTest {
                    @Test
                    fun testOne() {
                        assertTrue(true)
                    }

                    @Test
                    fun testTwo() {
                        assertTrue(true)
                    }
                }
                """.trimIndent()
            }

            build(":allTests") {
                assertTasksExecuted(":jvmTest", ":jsNodeTest", ":allTests")
            }

            assertFileInProjectExists("build/reports/tests/allTests/index.html")
            assertExecutedTestCases(
                ":jvmTest",
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
            )
            assertExecutedTestCases(
                ":jsNodeTest",
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
            )
        }
    }
}

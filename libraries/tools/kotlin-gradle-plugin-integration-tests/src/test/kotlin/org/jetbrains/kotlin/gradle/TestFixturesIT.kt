/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Integration with the Gradle java-test-fixtures plugin")
class TestFixturesIT : KGPBaseTest() {
    @DisplayName("Test fixtures can access internals of the main source set in Kotlin/JVM projects")
    @JvmGradlePluginTests
    @GradleTest
    fun testInternalAccessInJvmProject(gradleVersion: GradleVersion) {
        project(JVM_TEST_FIXTURES_PROJECT_NAME, gradleVersion) {
            kotlinSourcesDir("testFixtures").resolve("Netherlands.kt").appendText(
                //language=kt
                """

                    fun isCityFromNetherlands(city: City) = city.isNetherlands()
                """.trimIndent()
            )

            build("compileTestFixturesKotlin")
        }
    }

    @DisplayName("Test fixtures can access internals of the main JVM source set in Kotlin MPP projects")
    @MppGradlePluginTests
    @GradleTest
    fun testInternalAccessInMppProject(gradleVersion: GradleVersion) {
        project(MPP_TEST_FIXTURES_PROJECT_NAME, gradleVersion) {
            kotlinSourcesDir("jvmTestFixtures").resolve("Netherlands.kt").appendText(
                //language=kt
                """

                    fun isCityFromNetherlands(city: City) = city.isNetherlands()
                """.trimIndent()
            )

            build("compileTestFixturesKotlinJvm")
        }
    }

    @DisplayName("Test code can access internals of the test fixtures source set in Kotlin/JVM projects")
    @JvmGradlePluginTests
    @GradleTest
    fun testInternalAccessFromTestsInJvmProject(gradleVersion: GradleVersion) {
        project(JVM_TEST_FIXTURES_PROJECT_NAME, gradleVersion) {
            kotlinSourcesDir("testFixtures").resolve("Netherlands.kt").appendText(
                //language=kt
                """

                    internal fun isCityFromNetherlands(city: City) = city.isNetherlands()
                """.trimIndent()
            )

            kotlinSourcesDir("test").resolve("Tests.kt").modify {
                it.replace(
                    "assertEquals(true, AMSTERDAM.isNetherlands())",
                    "assertEquals(AMSTERDAM.isNetherlands(), isCityFromNetherlands(AMSTERDAM))"
                )
            }

            build("compileTestKotlin")
        }
    }

    @DisplayName("JVM test code can access internals of the test fixtures source set in Kotlin MPP projects")
    @MppGradlePluginTests
    @GradleTest
    fun testInternalAccessFromTestsInMppProject(gradleVersion: GradleVersion) {
        project(MPP_TEST_FIXTURES_PROJECT_NAME, gradleVersion) {
            kotlinSourcesDir("jvmTestFixtures").resolve("Netherlands.kt").appendText(
                //language=kt
                """

                    internal fun isCityFromNetherlands(city: City) = city.isNetherlands()
                """.trimIndent()
            )

            kotlinSourcesDir("jvmTest").resolve("Tests.kt").modify {
                it.replace(
                    "assertEquals(true, AMSTERDAM.isNetherlands())",
                    "assertEquals(AMSTERDAM.isNetherlands(), isCityFromNetherlands(AMSTERDAM))"
                )
            }

            build("compileTestKotlinJvm")
        }
    }

    @DisplayName("Test associated 'functionalTest' compilation can compile and run with test and testFixtures in JVM project")
    @MppGradlePluginTests
    @GradleTest
    fun testTestFixturesAndFunctionalTestsInJvmProject(gradleVersion: GradleVersion) {
        project("jvm-test-fixtures-functionalTest", gradleVersion) {
            build("functionalTest") {
                assertOutputContains("src/main OK!")
                assertOutputContains("src/test OK!")
                assertOutputContains("src/testFixtures OK!")
                assertOutputContains("src/functionalTest OK!")
            }
        }
    }

    @DisplayName("Test associated 'functionalTest' compilation can compile and run with test and testFixtures in Multiplatform project")
    @MppGradlePluginTests
    @GradleTest
    fun testTestFixturesAndFunctionalTestsInMppProject(gradleVersion: GradleVersion) {
        project("mpp-test-fixtures-functionalTest", gradleVersion) {
            build("functionalTest") {
                assertOutputContains("src/main OK!")
                assertOutputContains("src/test OK!")
                assertOutputContains("src/testFixtures OK!")
                assertOutputContains("src/functionalTest OK!")
            }
        }
    }


    companion object {
        private const val JVM_TEST_FIXTURES_PROJECT_NAME = "jvm-test-fixtures"
        private const val MPP_TEST_FIXTURES_PROJECT_NAME = "mpp-test-fixtures"
    }
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Integration with the Gradle java-test-fixtures plugin")
class TestFixturesIT : KGPBaseTest() {
    @DisplayName("Test fixtures can access internals of the main source set in Kotlin/JVM projects")
    @JvmGradlePluginTests
    @GradleTest
    @TestMetadata(JVM_TEST_FIXTURES_PROJECT_NAME)
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

    @DisplayName("Test fixtures can access internals of the main JVM source set in KMP projects")
    @MppGradlePluginTests
    @GradleTest
    @TestMetadata(KMP_TEST_FIXTURES_PROJECT_NAME)
    fun testInternalAccessInKmpProject(gradleVersion: GradleVersion) {
        project(KMP_TEST_FIXTURES_PROJECT_NAME, gradleVersion) {
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
    @TestMetadata(JVM_TEST_FIXTURES_PROJECT_NAME)
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

    @DisplayName("JVM test code can access internals of the test fixtures source set in KMP projects")
    @MppGradlePluginTests
    @GradleTest
    @TestMetadata(KMP_TEST_FIXTURES_PROJECT_NAME)
    fun testInternalAccessFromTestsInKmpProject(gradleVersion: GradleVersion) {
        project(KMP_TEST_FIXTURES_PROJECT_NAME, gradleVersion) {
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
    @TestMetadata(KMP_TEST_FIXTURES_WITH_FUNCTIONAL_TEST_PROJECT_NAME)
    fun testTestFixturesAndFunctionalTestsInJvmProject(gradleVersion: GradleVersion) {
        project(KMP_TEST_FIXTURES_WITH_FUNCTIONAL_TEST_PROJECT_NAME, gradleVersion) {
            build("functionalTest") {
                assertOutputContains("src/main OK!")
                assertOutputContains("src/test OK!")
                assertOutputContains("src/testFixtures OK!")
                assertOutputContains("src/functionalTest OK!")
            }

            // Workaround for Junit 'Failed to delete temp directory' on Windows OS
            build("clean")
        }
    }

    @DisplayName("Test associated 'functionalTest' compilation can compile and run with test and testFixtures in KMP project")
    @MppGradlePluginTests
    @GradleTest
    @TestMetadata(KMP_TEST_FIXTURES_WITH_FUNCTIONAL_TEST_PROJECT_NAME)
    fun testTestFixturesAndFunctionalTestsInKmpProject(gradleVersion: GradleVersion) {
        project(KMP_TEST_FIXTURES_WITH_FUNCTIONAL_TEST_PROJECT_NAME, gradleVersion) {
            build("functionalTest") {
                assertOutputContains("src/main OK!")
                assertOutputContains("src/test OK!")
                assertOutputContains("src/testFixtures OK!")
                assertOutputContains("src/functionalTest OK!")
            }

            // Workaround for Junit 'Failed to delete temp directory' on Windows OS
            build("clean")
        }
    }

    @DisplayName("KT-75188: Test code can access internals of the test fixtures source set in Kotlin/JVM projects with Groovy")
    @JvmGradlePluginTests
    @GradleTest
    @TestMetadata(JVM_TEST_FIXTURES_PROJECT_NAME)
    fun testInternalAccessInJvmProjectWithGroovy(gradleVersion: GradleVersion) {
        project(JVM_TEST_FIXTURES_PROJECT_NAME, gradleVersion) {
            buildScriptInjection {
                project.plugins.apply("groovy")
            }
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

    @DisplayName("KT-75808: Correct project dependency on testFixtures")
    @MppGradlePluginTests
    @GradleTest
    @TestMetadata(KMP_JVM_TEST_FIXTURES_PROJECT_NAME)
    fun testProjectDependencyOnKmpTestFixtures(gradleVersion: GradleVersion) {
        project(KMP_JVM_TEST_FIXTURES_PROJECT_NAME, gradleVersion) {
            build(":lib:testClasses")
        }
    }

    companion object {
        private const val JVM_TEST_FIXTURES_PROJECT_NAME = "jvm-test-fixtures"
        private const val KMP_TEST_FIXTURES_PROJECT_NAME = "mpp-test-fixtures"
        private const val KMP_TEST_FIXTURES_WITH_FUNCTIONAL_TEST_PROJECT_NAME = "mpp-test-fixtures-functionalTest"
        private const val KMP_JVM_TEST_FIXTURES_PROJECT_NAME = "kmp-jvm-test-fixtures"
    }
}

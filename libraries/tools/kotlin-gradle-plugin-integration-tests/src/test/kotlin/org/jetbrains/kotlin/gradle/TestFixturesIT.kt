/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.exists

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

    @DisplayName("Test fixtures can access internals of the main JVM source set in KMP projects")
    @MppGradlePluginTests
    @GradleTest
    fun testInternalAccessInMppProjectWithJava(gradleVersion: GradleVersion) {
        project(MPP_TEST_FIXTURES_PROJECT_NAME, gradleVersion) {
            applyJavaPluginIfRequired(gradleVersion)
            kotlinSourcesDir("jvmTestFixtures").resolve("Netherlands.kt").appendText(
                //language=kt
                """

                    fun isCityFromNetherlands(city: City) = city.isNetherlands()
                """.trimIndent()
            )

            build("compileTestFixturesKotlinJvm")
        }
    }

    @DisplayName("Test fixtures can access internals of the main JVM source set in Kotlin MPP projects")
    @MppGradlePluginTests
    @GradleTest
    fun testInternalAccessInMppProject(gradleVersion: GradleVersion) {
        project(MPP_TEST_FIXTURES_PROJECT_NAME, gradleVersion) {
            applyJavaPluginIfRequired(gradleVersion)
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
            applyJavaPluginIfRequired(gradleVersion)
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
        project(MPP_TEST_FIXTURES_WITH_FUNCTIONAL_TEST_PROJECT_NAME, gradleVersion) {
            applyJavaPluginIfRequired(gradleVersion)
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
        project(MPP_TEST_FIXTURES_WITH_FUNCTIONAL_TEST_PROJECT_NAME, gradleVersion) {
            applyJavaPluginIfRequired(gradleVersion)
            build("functionalTest") {
                assertOutputContains("src/main OK!")
                assertOutputContains("src/test OK!")
                assertOutputContains("src/testFixtures OK!")
                assertOutputContains("src/functionalTest OK!")
            }
        }
    }

    /**
     * The `java` plugin is not required for `java-test-fixtures` since Gradle 8.4:
     * https://github.com/gradle/gradle/commit/7cc5e4f695e42ecbdd3cc938a2a4dd7ade01ad2a
     */
    private fun TestProject.applyJavaPluginIfRequired(gradleVersion: GradleVersion) {
        if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_4)) {
            if (buildGradle.exists()) {
                buildGradle.modify {
                    it.replace("id(\"java-test-fixtures\")", "id(\"java\")\nid(\"java-test-fixtures\")")
                }
            } else {
                buildGradleKts.modify {
                    it.replace("`java-test-fixtures`", "java\n`java-test-fixtures`")
                }
            }
        }
    }


    companion object {
        private const val JVM_TEST_FIXTURES_PROJECT_NAME = "jvm-test-fixtures"
        private const val MPP_TEST_FIXTURES_PROJECT_NAME = "mpp-test-fixtures"
        private const val MPP_TEST_FIXTURES_WITH_FUNCTIONAL_TEST_PROJECT_NAME = "mpp-test-fixtures-functionalTest"
    }
}
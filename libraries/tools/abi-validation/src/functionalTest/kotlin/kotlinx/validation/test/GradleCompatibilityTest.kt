/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import org.assertj.core.api.Assertions
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assume
import org.junit.Test
import kotlin.test.assertTrue

class GradleCompatibilityTest : BaseKotlinGradleTest() {

    @Test
    fun test8Dot0() {
        checkDumpWithGradle("8.0")
    }

    @Test
    fun test7Dot0() {
        checkDumpWithGradle("7.0")
    }

    @Test
    fun testMin() {
        val runner = test(gradleVersion = "6.1.1", injectPluginClasspath = false) {
            buildGradleKts {
                resolve("/examples/gradle/base/withPluginMinKotlin.gradle.kts")
            }
            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }

            runner(withConfigurationCache = false) {
                arguments.add(":apiDump")
            }
        }

        skipInDebug(runner)

        runner.build().apply {
            assertTaskSuccess(":apiDump")

            assertTrue(rootProjectApiDump.exists(), "api dump file should exist")

            val expected = readFileList("/examples/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }

    private fun skipInDebug(runner: GradleRunner) {
        Assume.assumeFalse(
            "The test requires a separate Gradle distributive " +
                    "so it could not be executed with debug turned on.",
            runner.isDebug
        )
    }

    private fun checkDumpWithGradle(gradleVersion: String) {
        val runner = test(gradleVersion = gradleVersion, injectPluginClasspath = false) {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
            }
            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }

            runner {
                arguments.add(":apiDump")
            }
        }

        skipInDebug(runner)

        runner.build().apply {
            assertTaskSuccess(":apiDump")

            assertTrue(rootProjectApiDump.exists(), "api dump file should exist")

            val expected = readFileList("/examples/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }

}

/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test.legacy

import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.abi.tools.test.api.*
import org.junit.Assume
import org.junit.Test
import kotlin.test.assertTrue

class GradleCompatibilityTest : BaseKotlinGradleTest() {

    @Test
    fun test8Dot0() {
        checkDumpWithGradle("8.0")
    }

    @Test
    fun testMin() {
        checkDumpWithGradle("7.6.3")
    }

    private fun skipInDebug(runner: GradleRunner) {
        Assume.assumeFalse(
            "The test requires a separate Gradle distributive " +
                    "so it could not be executed with debug turned on.",
            runner.isDebug
        )
    }

    private fun checkDumpWithGradle(gradleVersion: String) {
        val runner = test(gradleVersion = gradleVersion) {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
            }
            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }

            runner {
                arguments.add(":updateLegacyAbi")
            }
        }

        skipInDebug(runner)

        runner.build().apply {
            assertTaskSuccess(":updateLegacyAbi")

            assertTrue(rootProjectApiDump.exists(), "api dump file should exist")

            val expected = readFileList("/examples/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }

}

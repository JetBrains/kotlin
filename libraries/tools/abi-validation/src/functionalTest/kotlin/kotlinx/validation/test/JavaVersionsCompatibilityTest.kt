/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import kotlinx.validation.api.buildGradleKts
import kotlinx.validation.api.resolve
import kotlinx.validation.api.test
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assume
import org.junit.Test

class JavaVersionsCompatibilityTest : BaseKotlinGradleTest() {
    private fun skipInDebug(runner: GradleRunner) {
        Assume.assumeFalse(
            "The test requires a separate Gradle process as it uses a different JVM version, " +
                    "so it could not be executed with debug turned on.",
            runner.isDebug
        )
    }

    private fun checkCompatibility(useMaxVersion: Boolean) {
        val runner = test(gradleVersion = "8.5", injectPluginClasspath = false) {
            buildGradleKts {
                resolve("/examples/gradle/base/jdkCompatibility.gradle.kts")
            }
            settingsGradleKts {
                resolve("/examples/gradle/settings/jdk-provisioning.gradle.kts")
            }
            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }
            apiFile(projectName = rootProjectDir.name) {
                resolve("/examples/classes/AnotherBuildConfig.dump")
            }

            runner {
                arguments.add("-PuseMaxVersion=$useMaxVersion")
                arguments.add(":apiCheck")
            }
        }

        skipInDebug(runner)

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }

    private fun checkCompatibility(jdkVersion: String) {
        val runner = test(gradleVersion = "8.5", injectPluginClasspath = false) {
            buildGradleKts {
                resolve("/examples/gradle/base/jdkCompatibilityWithExactVersion.gradle.kts")
            }
            settingsGradleKts {
                resolve("/examples/gradle/settings/jdk-provisioning.gradle.kts")
            }
            kotlin("AnotherBuildConfig.kt") {
                resolve("/examples/classes/AnotherBuildConfig.kt")
            }
            apiFile(projectName = rootProjectDir.name) {
                resolve("/examples/classes/AnotherBuildConfig.dump")
            }

            runner {
                arguments.add("-PjdkVersion=$jdkVersion")
                arguments.add(":apiCheck")
            }
        }

        skipInDebug(runner)

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }

    @Test
    fun testMaxSupportedVersion() {
        checkCompatibility(true)
    }

    @Test
    fun testMinSupportedVersion() {
        checkCompatibility(false)
    }

    @Test
    fun testLts8() {
        checkCompatibility("1.8")
    }

    @Test
    fun testLts11() {
        checkCompatibility("11")
    }

    @Test
    fun testLts17() {
        checkCompatibility("17")
    }

    @Test
    fun testLts21() {
        checkCompatibility("21")
    }
}

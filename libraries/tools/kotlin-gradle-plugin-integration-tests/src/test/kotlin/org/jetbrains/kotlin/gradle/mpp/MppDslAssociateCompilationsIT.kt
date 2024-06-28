/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.TaskOutcome.*
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.capitalize
import org.jetbrains.kotlin.gradle.util.checkBytecodeContains
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled
import kotlin.test.assertTrue

@MppGradlePluginTests
class MppDslAssociateCompilationsIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-associate-compilations")
    fun testAssociateCompilations(gradleVersion: GradleVersion) {
        project(
            projectName = "new-mpp-associate-compilations",
            gradleVersion = gradleVersion,
        ) {
            val tasks = arrayOf(
                ":compileIntegrationTestKotlinJvm",
                ":compileIntegrationTestKotlinJs",
                ":compileIntegrationTestKotlinLinux64",
            )

            build(*tasks) {
                assertTasksExecuted(*tasks)

                // JVM:
                checkBytecodeContains(
                    projectPath.resolve("build/classes/kotlin/jvm/integrationTest/com/example/HelloIntegrationTest.class").toFile(),
                    "Hello.internalFun\$new_mpp_associate_compilations",
                    "HelloTest.internalTestFun\$new_mpp_associate_compilations"
                )
                assertFileInProjectExists("build/classes/kotlin/jvm/integrationTest/META-INF/new-mpp-associate-compilations_integrationTest.kotlin_module")

                // JS:
                assertFileInProjectExists("build/classes/kotlin/js/integrationTest/default/manifest")

                // Native:
                assertFileInProjectExists("build/classes/kotlin/linux64/integrationTest/klib/new-mpp-associate-compilations_integrationTest.klib")
            }
        }
    }

    @GradleTest
    @TestMetadata(value = "new-mpp-associate-compilations")
    fun `test testRuns API for JVM`(gradleVersion: GradleVersion) {
        testTestRunsApi(
            gradleVersion = gradleVersion,
            targetName = "jvm",
            expectedExecutedTasks = listOf(":compileIntegrationTestKotlinJvm")
        )
    }

    @GradleTest
    @TestMetadata(value = "new-mpp-associate-compilations")
    @Disabled("KT-68454 custom test runs for JS don't work")
    fun `test testRuns API for JS`(gradleVersion: GradleVersion) {
        testTestRunsApi(
            gradleVersion = gradleVersion,
            targetName = "js",
        )
    }

    @GradleTest
    @TestMetadata(value = "new-mpp-associate-compilations")
    fun `test testRuns API for iOS`(gradleVersion: GradleVersion) {
        val iosTarget = when (HostManager.host) {
            KonanTarget.MACOS_X64 -> "iosX64"
            KonanTarget.MACOS_ARM64 -> "iosSimulatorArm64"
            else -> null
        }
        assumeTrue(iosTarget != null)
        requireNotNull(iosTarget)

        testTestRunsApi(
            gradleVersion = gradleVersion,
            targetName = iosTarget,
        )
    }

    @GradleTest
    @TestMetadata(value = "new-mpp-associate-compilations")
    fun `test testRuns API for HostTarget`(gradleVersion: GradleVersion) {
        val nativeHostTargetName = MPPNativeTargets.current
        testTestRunsApi(
            gradleVersion = gradleVersion,
            targetName = MPPNativeTargets.current,
            expectedExecutedTasks = listOf(":linkIntegrationDebugTest${nativeHostTargetName.capitalize()}"),
        )
    }

    private fun testTestRunsApi(
        gradleVersion: GradleVersion,
        targetName: String,
        expectedExecutedTasks: List<String>? = null,
        testTasks: List<String> = listOf(":${targetName}Test", ":${targetName}IntegrationTest"),
    ) {
        project(
            projectName = "new-mpp-associate-compilations",
            gradleVersion = gradleVersion,
        ) {
            build(
                buildArguments = testTasks.toTypedArray(),
                enableBuildCacheDebug = false,
                buildOptions = buildOptions.copy(
                    logLevel = LogLevel.LIFECYCLE,
                    buildCacheEnabled = true,
                    configurationCache = gradleVersion >= GradleVersion.version("8.5"),
                )
            ) {
                assertTasksExecuted(testTasks)

                expectedExecutedTasks?.forEach { expecetedTask ->
                    val executedTask = task(expecetedTask)
                    assertTrue(
                        executedTask?.outcome in listOf(SUCCESS, UP_TO_DATE, FROM_CACHE),
                        "Expected task outcome $executedTask was successful, but was ${executedTask?.outcome}"
                    )
                }

                val testReportFile = "build/reports/tests/${targetName}Test/classes/com.example.HelloTest.html"

                assertFileInProjectDoesNotContain(testReportFile, "secondTest")

                val integrationTestReportFile =
                    "build/reports/tests/${targetName}IntegrationTest/classes/com.example.HelloIntegrationTest.html"

                assertFileInProjectContains(integrationTestReportFile, "test[$targetName]")
                assertFileInProjectDoesNotContain(integrationTestReportFile, "secondTest")
                assertFileInProjectDoesNotContain(integrationTestReportFile, "thirdTest")
            }
        }
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.internals.DISABLED_NATIVE_TARGETS_REPORTER_WARNING_PREFIX
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectories
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class CommonizerIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun `test commonizeNativeDistributionWithIosLinuxWindows`() {
        with(Project("commonizeNativeDistributionWithIosLinuxWindows")) {
            build(":p1:commonize") {
                assertTasksExecuted(":p1:commonizeNativeDistribution")
                assertContains(DISABLED_NATIVE_TARGETS_REPORTER_WARNING_PREFIX)
                assertSuccessful()
            }
        }
    }

    @Test
    fun `test commonizeCurlInterop UP-TO-DATE check`() {
        with(preparedProject("commonizeCurlInterop")) {
            build(":commonize") {
                assertTasksExecuted(":commonizeNativeDistribution")
                assertTasksExecuted(":cinteropCurlTargetA")
                assertTasksExecuted(":cinteropCurlTargetB")
                assertTasksExecuted(":commonizeCInterop")
                assertSuccessful()
            }

            build(":commonize") {
                assertTasksUpToDate(":commonizeNativeDistribution")
                assertTasksUpToDate(":cinteropCurlTargetA")
                assertTasksUpToDate(":cinteropCurlTargetB")
                assertTasksUpToDate(":commonizeCInterop")
                assertSuccessful()
            }

            val buildGradleKts = projectFile("build.gradle.kts")
            val originalBuildGradleKtsContent = buildGradleKts.readText()

            buildGradleKts.writeText(originalBuildGradleKtsContent.replace("curl", "curl2"))
            build(":commonize") {
                assertTasksUpToDate(":commonizeNativeDistribution")
                assertTasksExecuted(":cinteropCurl2TargetA")
                assertTasksExecuted(":cinteropCurl2TargetB")
                assertTasksExecuted(":commonizeCInterop")
                assertSuccessful()
            }

            buildGradleKts.writeText(originalBuildGradleKtsContent.lineSequence().filter { "curl" !in it }.joinToString("\n"))
            build(":commonize") {
                assertTasksUpToDate(":commonizeNativeDistribution")
                assertTasksNotExecuted(":cinteropCurlTargetA")
                assertTasksNotExecuted(":cinteropCurlTargetB")
                assertSuccessful()
            }
        }
    }

    @Test
    fun `test commonizeCurlInterop feature flag`() {
        with(preparedProject("commonizeCurlInterop")) {
            setupWorkingDir()
            // Remove feature flag from gradle.properties
            projectFile("gradle.properties").apply {
                writeText(readText().lineSequence().filter { "enableCInteropCommonization" !in it }.joinToString("\n"))
            }

            build(":commonize") {
                assertTasksExecuted(":commonizeNativeDistribution")
                assertTasksNotExecuted(":cinteropCurl2TargetA")
                assertTasksNotExecuted(":cinteropCurl2TargetB")
                assertTasksNotExecuted(":commonizeCInterop")
                assertSuccessful()
            }

            build(":commonize", "-Pkotlin.mpp.enableCInteropCommonization=true") {
                assertTasksUpToDate(":commonizeNativeDistribution")
                assertTasksExecuted(":cinteropCurlTargetA")
                assertTasksExecuted(":cinteropCurlTargetB")
                assertTasksExecuted(":commonizeCInterop")
                assertSuccessful()
            }

            build(":commonize", "-Pkotlin.mpp.enableCInteropCommonization=false") {
                assertTasksUpToDate(":commonizeNativeDistribution")
                assertTasksNotExecuted(":cinteropCurlTargetA")
                assertTasksNotExecuted(":cinteropCurlTargetB")
                assertTasksNotExecuted(":commonizeCInterop")
                assertSuccessful()
            }
        }
    }

    @Test
    fun `test commonizeCurlInterop copyCommonizeCInteropForIde`() {
        with(preparedProject("commonizeCurlInterop")) {
            setupWorkingDir()
            val expectedOutputDirectoryForIde = projectDir.resolve(".gradle/kotlin/commonizer")
            val expectedOutputDirectoryForBuild = projectDir.resolve("build/classes/kotlin/commonizer")

            build(":copyCommonizeCInteropForIde") {
                assertTasksExecuted(":cinteropCurlTargetA")
                assertTasksExecuted(":cinteropCurlTargetB")
                assertTasksExecuted(":commonizeCInterop")
                assertSuccessful()

                assertTrue(expectedOutputDirectoryForIde.isDirectory, "Missing output directory for IDE")
                assertTrue(expectedOutputDirectoryForBuild.isDirectory, "Missing output directory for build")
                assertEqualDirectories(expectedOutputDirectoryForBuild, expectedOutputDirectoryForIde, false)
            }

            build(":clean") {
                assertSuccessful()
                assertTrue(expectedOutputDirectoryForIde.isDirectory, "Expected ide output directory to survive cleaning")
                assertFalse(expectedOutputDirectoryForBuild.exists(), "Expected output directory for build to be cleaned")
            }
        }
    }

    @Test
    fun `test commonizeCurlInterop compilation`() {
        with(preparedProject("commonizeCurlInterop")) {
            build(":compileNativeMainKotlinMetadata") {
                assertTasksExecuted(":commonizeNativeDistribution")
                assertTasksExecuted(":cinteropCurlTargetA")
                assertTasksExecuted(":cinteropCurlTargetB")
                assertTasksExecuted(":commonizeCInterop")
                assertSuccessful()
            }

            if (CommonizableTargets.targetA.isCompilable) {
                // targetA will be macos
                build(":targetABinaries") {
                    assertSuccessful()
                }
            }
            if (CommonizableTargets.targetB.isCompilable) {
                //targetB will be linuxArm64
                build(":targetBBinaries") {
                    assertSuccessful()
                }
            }
        }
    }

    @Test
    fun `test commonizeCurlInterop execution`() {
        with(preparedProject("commonizeCurlInterop")) {
            if (CommonizableTargets.targetA.isExecutable) {
                build(":targetATest") {
                    assertSuccessful()
                }
            }
            if (CommonizableTargets.targetB.isExecutable) {
                build(":targetBTest") {
                    assertSuccessful()
                }
            }
        }
    }

    @Test
    fun `test commonizeSQLiteInterop`() {
        with(preparedProject("commonizeSQLiteInterop")) {
            build(":commonize") {
                assertSuccessful()
                assertTasksExecuted(":cinteropSqliteTargetA")
                assertTasksExecuted(":cinteropSqliteTargetB")
                assertTasksExecuted(":commonizeCInterop")
            }
        }
    }

    @Test
    fun `test commonizeSQLiteAndCurlInterop`() {
        with(preparedProject("commonizeSQLiteAndCurlInterop")) {
            build(":commonize") {
                assertSuccessful()
                assertTasksExecuted(":cinteropSqliteTargetA")
                assertTasksExecuted(":cinteropSqliteTargetB")
                assertTasksExecuted(":cinteropCurlTargetA")
                assertTasksExecuted(":cinteropCurlTargetB")
                assertTasksExecuted(":commonizeCInterop")
            }

            build(":compileNativeMainKotlinMetadata") {
                assertSuccessful()
                assertTasksUpToDate(":cinteropSqliteTargetA")
                assertTasksUpToDate(":cinteropSqliteTargetB")
                assertTasksUpToDate(":cinteropCurlTargetA")
                assertTasksUpToDate(":cinteropCurlTargetB")
                assertTasksUpToDate(":commonizeCInterop")
            }
        }
    }

    @Test
    fun `test commonizeInterop using posix APIs`() {
        with(preparedProject("commonizeInteropUsingPosixApis")) {
            build(":commonizeCInterop") {
                assertSuccessful()
                assertTasksExecuted(":cinteropWithPosixTargetA")
                assertTasksExecuted(":cinteropWithPosixTargetB")
                assertTasksExecuted(":commonizeNativeDistribution")
                assertTasksExecuted(":commonizeCInterop")
            }

            build(":compileNativeMainKotlinMetadata") {
                assertSuccessful()
                assertTasksUpToDate(":cinteropWithPosixTargetA")
                assertTasksUpToDate(":cinteropWithPosixTargetB")
                assertTasksUpToDate(":commonizeNativeDistribution")
                assertTasksUpToDate(":commonizeCInterop")
            }
        }
    }

    private fun preparedProject(name: String): Project {
        return Project(name).apply {
            setupWorkingDir()
            projectDir.walkTopDown().filter { it.name.startsWith("build.gradle") }.forEach { buildFile ->
                val originalText = buildFile.readText()
                val preparedText = originalText
                    .replace("<targetA>", CommonizableTargets.targetA.value)
                    .replace("<targetB>", CommonizableTargets.targetB.value)
                buildFile.writeText(preparedText)
            }
        }
    }
}

private data class TargetSubstitution(val value: String, val isCompilable: Boolean, val isExecutable: Boolean) {
    override fun toString(): String = value
}

private object CommonizableTargets {
    private val os = OperatingSystem.current()

    val targetA = when {
        os.isMacOsX -> TargetSubstitution("macosX64", isCompilable = true, isExecutable = true)
        os.isLinux -> TargetSubstitution("linuxX64", isCompilable = true, isExecutable = true)
        os.isWindows -> TargetSubstitution("mingwX64", isCompilable = true, isExecutable = false)
        else -> fail("Unsupported os: ${os.name}")
    }

    val targetB = when {
        os.isMacOsX -> TargetSubstitution("linuxX64", isCompilable = true, isExecutable = false)
        os.isLinux -> TargetSubstitution("linuxArm64", isCompilable = true, isExecutable = false)
        os.isWindows -> TargetSubstitution("mingwX86", isCompilable = true, isExecutable = false)
        else -> fail("Unsupported os: ${os.name}")
    }
}


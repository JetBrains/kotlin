/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.internals.DISABLED_NATIVE_TARGETS_REPORTER_WARNING_PREFIX
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectories
import kotlin.test.*

class CommonizerIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.FOR_MPP_SUPPORT

    companion object {
        private const val commonizerOutput = "Preparing commonized Kotlin/Native libraries"
    }

    @Ignore // TODO NOW
    @Test
    fun `test commonizeNativeDistributionWithIosLinuxWindows`() {
        with(Project("commonizeNativeDistributionWithIosLinuxWindows")) {
            build(":p1:commonize", "-Pkotlin.mpp.enableNativeDistributionCommonizationCache=false") {
                assertTasksExecuted(":p1:commonizeNativeDistribution")
                assertContains(DISABLED_NATIVE_TARGETS_REPORTER_WARNING_PREFIX)
                assertContains(commonizerOutput)
                assertSuccessful()
            }

            build(":p1:commonize", "--rerun-tasks", "-Pkotlin.mpp.enableNativeDistributionCommonizationCache=true") {
                assertTasksExecuted(":p1:commonizeNativeDistribution")
                assertContains("Native Distribution Commonization: Cache hit")
                assertNotContains(commonizerOutput)
                assertSuccessful()
            }

            build(":p1:commonize", "--rerun-tasks", "-Pkotlin.mpp.enableNativeDistributionCommonizationCache=false") {
                assertTasksExecuted(":p1:commonizeNativeDistribution")
                assertContains("Native Distribution Commonization: Cache disabled")
                assertContains(commonizerOutput)
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

    @Test
    fun `test KT-46234 intermediate source set with only one native target`() {
        `test single native platform`("commonize-kt-46234-singleNativeTarget")
    }

    @Test
    fun `test KT-46142 standalone native source set`() {
        `test single native platform`("commonize-kt-46142-singleNativeTarget")
    }

    private fun `test single native platform`(project: String) {
        val posixInImplementationMetadataConfigurationRegex = Regex(""".*implementationMetadataConfiguration:.*([pP])osix""")
        val posixInIntransitiveMetadataConfigurationRegex = Regex(""".*intransitiveMetadataConfiguration:.*([pP])osix""")

        fun CompiledProject.containsPosixInImplementationMetadataConfiguration(): Boolean =
            output.lineSequence().any { line ->
                line.matches(posixInImplementationMetadataConfigurationRegex)
            }

        fun CompiledProject.containsPosixInIntransitiveMetadataConfiguration(): Boolean =
            output.lineSequence().any { line ->
                line.matches(posixInIntransitiveMetadataConfigurationRegex)
            }

        with(Project(project)) {
            build(":p1:listNativePlatformMainDependencies", "-Pkotlin.mpp.enableIntransitiveMetadataConfiguration=false") {
                assertSuccessful()

                assertTrue(
                    containsPosixInImplementationMetadataConfiguration(),
                    "Expected dependency on posix in implementationMetadataConfiguration"
                )

                assertFalse(
                    containsPosixInIntransitiveMetadataConfiguration(),
                    "Expected **no** dependency on posix in intransitiveMetadataConfiguration"
                )
            }

            build(":p1:listNativePlatformMainDependencies", "-Pkotlin.mpp.enableIntransitiveMetadataConfiguration=true") {
                assertSuccessful()

                assertFalse(
                    containsPosixInImplementationMetadataConfiguration(),
                    "Expected **no** posix dependency in implementationMetadataConfiguration"
                )

                assertTrue(
                    containsPosixInIntransitiveMetadataConfiguration(),
                    "Expected dependency on posix in intransitiveMetadataConfiguration"
                )
            }

            build("assemble") {
                assertSuccessful()
            }
        }
    }

    @Test
    fun `test KT-46248 single supported native target dependency propagation`() {
        fun CompiledProject.containsPosixDependency(): Boolean = output.lineSequence().any { line ->
            line.matches(Regex(""".*Dependency:.*[pP]osix"""))
        }

        with(Project("commonize-kt-46248-singleNativeTargetPropagation")) {
            build(":p1:listNativeMainDependencies") {
                assertSuccessful()
                assertTrue(containsPosixDependency(), "Expected dependency on posix in nativeMain")
            }

            build(":p1:listNativeMainParentDependencies") {
                assertSuccessful()
                assertTrue(containsPosixDependency(), "Expected dependency on posix in nativeMainParent")
            }

            build(":p1:listCommonMainDependencies") {
                assertSuccessful()
                assertFalse(containsPosixDependency(), "Expected **no** dependency on posix in commonMain (because of jvm target)")
            }

            build("assemble") {
                assertSuccessful()
                assertTasksExecuted(":p1:compileCommonMainKotlinMetadata")
                assertTasksExecuted(":p1:compileKotlinNativePlatform")
            }
        }
    }

    @Test
    fun `test KT-46856 filename too long - all native targets configured`() {
        with(Project("commonize-kt-46856-all-targets")) {
            build(":commonize", options = BuildOptions(forceOutputToStdout = true)) {
                assertSuccessful()
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


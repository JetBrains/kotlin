/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.junit.jupiter.api.condition.OS
import java.io.File
import kotlin.test.fail

/**
 * Runs Tests on a Gradle project with three subprojects
 *
 * p1: Depends on two cinterops (cinterop-simple & cinterop-withPosix) that will get commonized
 * p2: Depends on p1 (either as project or repository dependency)
 * p3: Depends on p2 (and has slightly different source set layout)
 *
 * The tests can run in two modes
 * - dependency-mode=project: In this case p2 will just declare a regular project dependency on p1
 * - dependency-mode=repository: In this case p2 will rely on a previously published version of p1
 */
@MppGradlePluginTests
open class MppCInteropDependencyTransformationIT : KGPBaseTest() {

    final override val defaultBuildOptions = super.defaultBuildOptions.copy(
        warningMode = WarningMode.Fail,
        parallel = true,
        freeCommandLineArgs = super.defaultBuildOptions.freeCommandLineArgs + "-s",
    )

    protected val projectDependencyOptions = defaultBuildOptions.copy(
        freeCommandLineArgs = defaultBuildOptions.freeCommandLineArgs + "-PdependencyMode=project"
    )

    protected val repositoryDependencyOptions = defaultBuildOptions.copy(
        freeCommandLineArgs = defaultBuildOptions.freeCommandLineArgs + "-PdependencyMode=repository"
    )

    @MppGradlePluginTests
    class ComplexProject : MppCInteropDependencyTransformationIT() {
        private fun getProject(gradleVersion: GradleVersion) = project("cinterop-MetadataDependencyTransformation", gradleVersion)

        @GradleTestWithOsCondition(enabledForCI = [OS.LINUX, OS.MAC])
        fun `test - compile project - dependencyMode=project`(gradleVersion: GradleVersion) {
            testCompileProject(getProject(gradleVersion), projectDependencyOptions) {
                assertProjectDependencyMode()
                assertTasksExecuted(":p1:commonizeCInterop")
            }
        }

        @GradleTestWithOsCondition(enabledForCI = [OS.LINUX, OS.MAC])
        fun `test - compile project - dependencyMode=repository`(gradleVersion: GradleVersion) {
            with(getProject(gradleVersion)) {
                publishP1ToBuildRepository()
                testCompileProject(this, repositoryDependencyOptions) {
                    assertRepositoryDependencyMode()
                }
            }
        }

        private fun testCompileProject(project: TestProject, buildOptions: BuildOptions, check: BuildResult.() -> Unit = {}) {
            project.build("compileAll", buildOptions = buildOptions) {
                check()

                /* Assert p2 & p3 compiled metadata */
                assertTasksExecuted(":p2:compileNativeMainKotlinMetadata")
                assertTasksExecuted(":p2:compileLinuxMainKotlinMetadata")
                assertTasksExecuted(":p3:compileNativeMainKotlinMetadata")

                if (HostManager.hostIsMac) {
                    assertTasksExecuted(":p2:compileAppleMainKotlinMetadata")
                    assertTasksExecuted(":p2:compileIosMainKotlinMetadata")
                    assertTasksExecuted(":p3:compileAppleAndLinuxMainKotlinMetadata")
                    assertTasksExecuted(":p3:compileIosMainKotlinMetadata")
                }

                if (HostManager.hostIsMingw || HostManager.hostIsMac) {
                    assertTasksExecuted(":p2:compileWindowsMainKotlinMetadata")
                    assertTasksExecuted(":p3:compileWindowsMainKotlinMetadata")
                }

                /* Assert p2 & p3 transformed cinterop dependencies */
                assertTasksExecuted(":p2:transformNativeMainCInteropDependenciesMetadata")
                assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")

                /* Assert p2 & p3 compiled tests */
                assertTasksExecuted(":p2:compileTestKotlinLinuxX64")
                assertTasksExecuted(":p3:compileTestKotlinLinuxX64")
            }
        }

        @GradleTestWithOsCondition(enabledForCI = [OS.LINUX, OS.MAC])
        fun `test - source set dependencies - dependencyMode=project`(gradleVersion: GradleVersion) {
            with(getProject(gradleVersion)) {
                reportSourceSetCommonizerDependencies(this, "p2", projectDependencyOptions) {
                    it.assertProjectDependencyMode()
                    it.assertTasksExecuted(":p2:transformNativeMainCInteropDependenciesMetadataForIde")
                    it.assertTasksNotExecuted(".*[cC]ompile.*")
                    assertP2SourceSetDependencies()
                }

                reportSourceSetCommonizerDependencies(this, "p3", projectDependencyOptions) {
                    it.assertProjectDependencyMode()
                    it.assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadataForIde")
                    it.assertTasksNotExecuted(".*[cC]ompile.*")
                    assertP3SourceSetDependencies()
                }
            }
        }

        @GradleTestWithOsCondition(enabledForCI = [OS.LINUX, OS.MAC])
        fun `test - source set dependencies - dependencyMode=repository`(gradleVersion: GradleVersion) {
            with(getProject(gradleVersion)) {
                publishP1ToBuildRepository()

                reportSourceSetCommonizerDependencies(this, "p2", repositoryDependencyOptions) {
                    it.assertRepositoryDependencyMode()
                    it.assertTasksExecuted(":p2:transformNativeMainCInteropDependenciesMetadataForIde")
                    it.assertTasksNotExecuted(".*[cC]ompile.*")
                    assertP2SourceSetDependencies()
                }

                reportSourceSetCommonizerDependencies(this, "p3", repositoryDependencyOptions) {
                    it.assertRepositoryDependencyMode()
                    it.assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadataForIde")
                    it.assertTasksNotExecuted(".*[cC]ompile.*")
                    assertP3SourceSetDependencies()
                }
            }
        }

        private fun WithSourceSetCommonizerDependencies.assertP2SourceSetDependencies() {
            listOf("nativeMain", "nativeTest").forEach { sourceSetName ->
                getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies()
                    .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                    .assertTargetOnAllDependencies(
                        CommonizerTarget(LINUX_ARM64, LINUX_X64, IOS_ARM64, IOS_X64, MACOS_X64, MINGW_X64, MINGW_X86)
                    )
            }

            if (HostManager.hostIsMac) {
                listOf("appleAndLinuxMain", "appleAndLinuxTest").forEach { sourceSetName ->
                    getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies()
                        .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                        .assertTargetOnAllDependencies(CommonizerTarget(LINUX_ARM64, LINUX_X64, IOS_ARM64, IOS_X64, MACOS_X64))
                }

                listOf("appleMain", "appleTest").forEach { sourceSetName ->
                    getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies()
                        .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                        .assertTargetOnAllDependencies(CommonizerTarget(IOS_ARM64, IOS_X64, MACOS_X64))
                }

                listOf("iosMain", "iosTest").forEach { sourceSetName ->
                    getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies()
                        .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                        .assertTargetOnAllDependencies(CommonizerTarget(IOS_ARM64, IOS_X64))
                }
            }

            if (HostManager.hostIsMingw || HostManager.hostIsMac) {
                listOf("windowsMain", "windowsTest").forEach { sourceSetName ->
                    getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies()
                        .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                        .assertTargetOnAllDependencies(CommonizerTarget(MINGW_X64, MINGW_X86))
                }

                listOf("linuxMain", "linuxTest").forEach { sourceSetName ->
                    getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies()
                        .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                        .assertTargetOnAllDependencies(CommonizerTarget(LINUX_ARM64, LINUX_X64))
                }
            }
        }

        private fun WithSourceSetCommonizerDependencies.assertP3SourceSetDependencies() {
            /*
            windowsAndLinuxMain / windowsAndLinuxTest will not have a 'perfect target match' in p1.
            They will choose cinterops associated with 'nativeMain'
             */
            listOf("nativeMain", "nativeTest", "windowsAndLinuxMain", "windowsAndLinuxTest").forEach { sourceSetName ->
                getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies()
                    .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                    .assertTargetOnAllDependencies(
                        CommonizerTarget(LINUX_ARM64, LINUX_X64, IOS_ARM64, IOS_X64, MACOS_X64, MINGW_X64, MINGW_X86)
                    )
            }

            if (HostManager.hostIsMac) {
                listOf("appleAndLinuxMain", "appleAndLinuxTest").forEach { sourceSetName ->
                    getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies()
                        .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                        .assertTargetOnAllDependencies(CommonizerTarget(LINUX_ARM64, LINUX_X64, IOS_ARM64, IOS_X64, MACOS_X64))
                }

                listOf("iosMain", "iosTest").forEach { sourceSetName ->
                    getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies()
                        .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                        .assertTargetOnAllDependencies(CommonizerTarget(IOS_ARM64, IOS_X64))
                }
            }

            if (HostManager.hostIsMingw || HostManager.hostIsMac) {
                listOf("windowsMain", "windowsTest").forEach { sourceSetName ->
                    getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies()
                        .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                        .assertTargetOnAllDependencies(CommonizerTarget(MINGW_X64, MINGW_X86))
                }
            }
        }

        @GradleTestWithOsCondition(enabledForCI = [OS.LINUX, OS.MAC])
        fun `test - transformation - UP-TO-DATE behaviour`(gradleVersion: GradleVersion) {
            with(getProject(gradleVersion)) {
                publishP1ToBuildRepository()
                build(":p3:transformNativeMainCInteropDependenciesMetadata", buildOptions = repositoryDependencyOptions) {
                    assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
                }

                build(":p3:transformNativeMainCInteropDependenciesMetadata", buildOptions = repositoryDependencyOptions) {
                    assertTasksNotExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
                    assertTasksUpToDate(":p3:transformNativeMainCInteropDependenciesMetadata")
                }

                val p3BuildGradleKts = projectDir.resolve("p3/build.gradle.kts")
                val p3BuildGradleKtsContent = p3BuildGradleKts.readText()

                // Remove dependency on p2 | Task should re-run
                p3BuildGradleKts.writeText(p3BuildGradleKtsContent.replace("""implementation(project(":p2"))""", ""))
                build(":p3:transformNativeMainCInteropDependenciesMetadata", buildOptions = repositoryDependencyOptions) {
                    assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
                }

                // Re-add dependency on p3 | Task should re-run for the next invocation
                p3BuildGradleKts.writeText(p3BuildGradleKtsContent)
                build(":p3:transformNativeMainCInteropDependenciesMetadata", buildOptions = repositoryDependencyOptions) {
                    assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
                }
                build(":p3:transformNativeMainCInteropDependenciesMetadata", buildOptions = repositoryDependencyOptions) {
                    assertTasksNotExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
                    assertTasksUpToDate(":p3:transformNativeMainCInteropDependenciesMetadata")
                }

                // Replace dependency to :p2 with coordinates directly
                p3BuildGradleKts.writeText(
                    p3BuildGradleKtsContent.replace("""project(":p2")""", """"kotlin-multiplatform-projects:p1:1.0.0-SNAPSHOT"""")
                )
                build(":p3:transformNativeMainCInteropDependenciesMetadata", buildOptions = repositoryDependencyOptions) {
                    assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
                }
            }
        }

        @GradleTestWithOsCondition(enabledForCI = [OS.LINUX, OS.MAC])
        fun `test - transformation - UP-TO-DATE behaviour - on removing and adding targets - on p2 - dependencyMode=repository`(
            gradleVersion: GradleVersion
        ) {
            with(getProject(gradleVersion)) {
                publishP1ToBuildRepository()
                `test - transformation - UP-TO-DATE behaviour - on removing and adding targets - on p2`(repositoryDependencyOptions)
            }
        }

        @GradleTestWithOsCondition(enabledForCI = [OS.LINUX, OS.MAC])
        fun `test - transformation - UP-TO-DATE behaviour - on removing and adding targets - on p2 - dependencyMode=project`(gradleVersion: GradleVersion) {
            with(getProject(gradleVersion)) {
                `test - transformation - UP-TO-DATE behaviour - on removing and adding targets - on p2`(projectDependencyOptions)
            }
        }

        private fun TestProject.`test - transformation - UP-TO-DATE behaviour - on removing and adding targets - on p2`(options: BuildOptions) {
            build(":p3:transformNativeMainCInteropDependenciesMetadata", buildOptions = options)

            build(":p3:transformNativeMainCInteropDependenciesMetadata", buildOptions = options) {
                assertTasksUpToDate(":p3:transformNativeMainCInteropDependenciesMetadata")
            }
        }
    }

    @MppGradlePluginTests
    class KT50952 : MppCInteropDependencyTransformationIT() {
        private fun getProject(gradleVersion: GradleVersion) = project("cinterop-MetadataDependencyTransformation-kt-50952", gradleVersion)

        @GradleTestWithOsCondition(enabledForCI = [OS.LINUX, OS.MAC])
        fun `test UP-TO-DATE - when changing consumer targets - dependencyMode=repository`(gradleVersion: GradleVersion) {
            val project = getProject(gradleVersion)
            project.publishP1ToBuildRepository()
            `test UP-TO-DATE - when changing consumer targets`(project, repositoryDependencyOptions)
        }

        @GradleTestWithOsCondition(enabledForCI = [OS.LINUX, OS.MAC])
        fun `test UP-TO-DATE - when changing consumer targets - dependencyMode=project`(gradleVersion: GradleVersion) {
            `test UP-TO-DATE - when changing consumer targets`(getProject(gradleVersion), projectDependencyOptions)
        }

        private fun `test UP-TO-DATE - when changing consumer targets`(project: TestProject, options: BuildOptions) {
            project.build(":p2:transformCommonMainCInteropDependenciesMetadata", buildOptions = options)

            project.build(":p2:transformCommonMainCInteropDependenciesMetadata", buildOptions = options) {
                assertTasksNotExecuted(":p2:transformCommonMainCInteropDependenciesMetadata")
                assertTasksUpToDate(":p2:transformCommonMainCInteropDependenciesMetadata")
            }

            val optionsWithAdditionalTargetEnabled = options.withFreeCommandLineArgument("-Pp2.enableLinuxArm32Hfp")

            project.build(":p2:transformCommonMainCInteropDependenciesMetadata", buildOptions = optionsWithAdditionalTargetEnabled) {
                assertTasksExecuted(":p2:transformCommonMainCInteropDependenciesMetadata")
            }

            project.build(":p2:transformCommonMainCInteropDependenciesMetadata", buildOptions = optionsWithAdditionalTargetEnabled) {
                assertTasksNotExecuted(":p2:transformCommonMainCInteropDependenciesMetadata")
                assertTasksUpToDate(":p2:transformCommonMainCInteropDependenciesMetadata")
            }

            project.build(":p2:transformCommonMainCInteropDependenciesMetadata", buildOptions = options) {
                assertTasksExecuted(":p2:transformCommonMainCInteropDependenciesMetadata")
            }

            project.build(":p2:transformCommonMainCInteropDependenciesMetadata", buildOptions = options) {
                assertTasksNotExecuted(":p2:transformCommonMainCInteropDependenciesMetadata")
                assertTasksUpToDate(":p2:transformCommonMainCInteropDependenciesMetadata")
            }
        }
    }

    protected fun TestProject.publishP1ToBuildRepository() {
        build(":p1:publishAllPublicationsToBuildRepository", buildOptions = repositoryDependencyOptions)
    }

    protected fun BuildResult.assertProjectDependencyMode() {
        assertOutputContains("dependencyMode = 'project'")
    }

    protected fun BuildResult.assertRepositoryDependencyMode() {
        assertOutputContains("dependencyMode = 'repository'")
    }
}

//ToDo move into another place after `CommonizerIT` will be migrated to `KGPBaseTest`
fun KGPBaseTest.reportSourceSetCommonizerDependencies(
    project: TestProject,
    subproject: String? = null,
    options: BuildOptions = defaultBuildOptions,
    test: WithSourceSetCommonizerDependencies.(buildResult: BuildResult) -> Unit
) = with(project) {

    gradleBuildScript(subproject).apply {
        appendText("\n\n")
        appendText(taskSourceCode)
        appendText("\n\n")
    }

    val taskName = buildString {
        if (subproject != null) append(":$subproject")
        append(":reportCommonizerSourceSetDependencies")
    }

    build(taskName, buildOptions = options) {
        val dependencyReports = output.lineSequence().filter { line -> line.contains("SourceSetCommonizerDependencyReport") }.toList()

        val withSourceSetCommonizerDependencies = WithSourceSetCommonizerDependencies { sourceSetName ->
            val reportMarker = "Report[$sourceSetName]"

            val reportForSourceSet = dependencyReports.firstOrNull { line -> line.contains(reportMarker) }
                ?: fail("Missing dependency report for $sourceSetName")

            val files = reportForSourceSet.split(reportMarker, limit = 2).last().split("|#+#|")
                .map(String::trim).filter(String::isNotEmpty).map(::File)

            val dependencies = files.mapNotNull { file -> createSourceSetCommonizerDependencyOrNull(sourceSetName, file) }.toSet()
            SourceSetCommonizerDependencies(sourceSetName, dependencies)
        }

        withSourceSetCommonizerDependencies.test(this)
    }
}

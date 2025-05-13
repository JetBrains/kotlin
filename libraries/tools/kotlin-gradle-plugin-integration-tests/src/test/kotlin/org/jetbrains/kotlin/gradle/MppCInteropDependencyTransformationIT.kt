/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.ConfigurationCacheValue
import org.jetbrains.kotlin.gradle.util.TaskInstantiationTrackingBuildService
import org.jetbrains.kotlin.gradle.util.WithSourceSetCommonizerDependencies
import org.jetbrains.kotlin.gradle.util.reportSourceSetCommonizerDependencies
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertTrue

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
@DisplayName("CInterop dependency transformation")
@NativeGradlePluginTests
class MppCInteropDependencyTransformationIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableKlibsCrossCompilation()

    private val projectDependencyMode = "-PdependencyMode=project"

    private val repositoryDependencyMode = "-PdependencyMode=repository"

    private fun BuildResult.assertProjectDependencyMode() {
        assertOutputContains("dependencyMode = 'project'")
    }

    private fun BuildResult.assertRepositoryDependencyMode() {
        assertOutputContains("dependencyMode = 'repository'")
    }

    private fun BuildResult.assertNoCompileTasksExecuted() {
        val compileTaskRegex = ".*[cC]ompile.*".toRegex()
        val taskPaths = tasks.map { it.path }
        assertTrue(taskPaths.none { it.contains(compileTaskRegex) })
    }

    private fun TestProject.publishP1ToBuildRepository() {
        build(":p1:publishAllPublicationsToMavenRepository", repositoryDependencyMode)
    }

    private val cinteropProjectName = "cinterop-MetadataDependencyTransformation"

    @DisplayName("Compile project with project mode")
    @GradleTest
    fun compileDependencyModeProject(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path,
    ) {
        testCompileProject(
            gradleVersion,
            projectDependencyMode,
            localRepo,
        ) {
            assertProjectDependencyMode()
            assertTasksExecuted(":p1:commonizeCInterop")
        }
    }

    @DisplayName("Compile project with repository mode")
    @GradleTest
    fun compileDependencyModeRepository(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path,
    ) {
        testCompileProject(
            gradleVersion,
            repositoryDependencyMode,
            localRepo,
            additionalBuildStep = { publishP1ToBuildRepository() }
        ) {
            assertRepositoryDependencyMode()
        }
    }

    private fun testCompileProject(
        gradleVersion: GradleVersion,
        repositoryMode: String,
        localRepo: Path,
        additionalBuildStep: TestProject.() -> Unit = {},
        check: BuildResult.() -> Unit = {},
    ) {
        project(
            projectName = cinteropProjectName,
            gradleVersion = gradleVersion,
            localRepoDir = localRepo
        ) {
            additionalBuildStep()

            build("compileAll", repositoryMode) {
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

                /* Assert p2 & p3 transformed cinterop dependencies */
                assertTasksExecuted(":p2:transformNativeMainCInteropDependenciesMetadata")
                assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")

                /* Assert p2 & p3 compiled for Windows */
                assertTasksExecuted(":p2:compileKotlinWindowsX64")
                assertTasksExecuted(":p3:compileKotlinWindowsX64")

                /* Assert p2 & p3 compiled tests */
                assertTasksExecuted(":p2:compileTestKotlinLinuxX64")
                assertTasksExecuted(":p3:compileTestKotlinLinuxX64")

                /* Configurations should not be resol ved during configuration phase */
                assertOutputDoesNotContain("Configuration resolved before Task Graph is ready")
            }
        }
    }

    @DisplayName("Source set dependency in project mode")
    @GradleTest
    fun sourceSetDependencyProjectMode(gradleVersion: GradleVersion) {
        project(cinteropProjectName, gradleVersion) {
            reportSourceSetCommonizerDependencies(
                subproject = "p2",
                options = defaultBuildOptions.copy(freeArgs = listOf(projectDependencyMode))
            ) {
                it.assertProjectDependencyMode()
                it.assertTasksExecuted(":p2:transformNativeMainCInteropDependenciesMetadataForIde")
                it.assertNoCompileTasksExecuted()
                assertP2SourceSetDependencies()
            }

            reportSourceSetCommonizerDependencies(
                subproject = "p3",
                options = defaultBuildOptions.copy(freeArgs = listOf(projectDependencyMode))
            ) {
                it.assertProjectDependencyMode()
                it.assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadataForIde")
                it.assertNoCompileTasksExecuted()
                assertP3SourceSetDependencies()
            }
        }
    }

    @DisplayName("Source set dependency in repository mode")
    @GradleTest
    fun sourceSetDependencyRepositoryMode(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path,
    ) {
        project(cinteropProjectName, gradleVersion, localRepoDir = localRepo) {
            publishP1ToBuildRepository()

            reportSourceSetCommonizerDependencies(
                subproject = "p2",
                options = defaultBuildOptions.copy(freeArgs = listOf(repositoryDependencyMode))
            ) {
                it.assertRepositoryDependencyMode()
                it.assertTasksExecuted(":p2:transformNativeMainCInteropDependenciesMetadataForIde")
                it.assertNoCompileTasksExecuted()
                assertP2SourceSetDependencies()
            }

            reportSourceSetCommonizerDependencies(
                subproject = "p3",
                options = defaultBuildOptions.copy(freeArgs = listOf(repositoryDependencyMode))
            ) {
                it.assertRepositoryDependencyMode()
                it.assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadataForIde")
                it.assertNoCompileTasksExecuted()
                assertP3SourceSetDependencies()
            }
        }
    }

    private fun WithSourceSetCommonizerDependencies.assertP2SourceSetDependencies() {
        listOf("nativeMain", "nativeTest").forEach { sourceSetName ->
            getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies(defaultBuildOptions.konanDataDir!!)
                .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                .assertTargetOnAllDependencies(
                    CommonizerTarget(
                        KonanTarget.LINUX_ARM64,
                        KonanTarget.LINUX_X64,
                        KonanTarget.IOS_ARM64,
                        KonanTarget.IOS_SIMULATOR_ARM64,
                        KonanTarget.IOS_X64,
                        KonanTarget.MACOS_X64,
                        KonanTarget.MINGW_X64
                    )
                )
        }

        if (HostManager.hostIsMac) {
            listOf("appleAndLinuxMain", "appleAndLinuxTest").forEach { sourceSetName ->
                getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies(defaultBuildOptions.konanDataDir!!)
                    .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                    .assertTargetOnAllDependencies(
                        CommonizerTarget(
                            KonanTarget.LINUX_ARM64,
                            KonanTarget.LINUX_X64,
                            KonanTarget.IOS_ARM64,
                            KonanTarget.IOS_SIMULATOR_ARM64,
                            KonanTarget.IOS_X64,
                            KonanTarget.MACOS_X64
                        )
                    )
            }

            listOf("appleMain", "appleTest").forEach { sourceSetName ->
                getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies(defaultBuildOptions.konanDataDir!!)
                    .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                    .assertTargetOnAllDependencies(
                        CommonizerTarget(
                            KonanTarget.IOS_ARM64,
                            KonanTarget.IOS_X64,
                            KonanTarget.IOS_SIMULATOR_ARM64,
                            KonanTarget.MACOS_X64
                        )
                    )
            }

            listOf("iosMain", "iosTest").forEach { sourceSetName ->
                getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies(defaultBuildOptions.konanDataDir!!)
                    .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                    .assertTargetOnAllDependencies(
                        CommonizerTarget(
                            KonanTarget.IOS_ARM64,
                            KonanTarget.IOS_X64,
                            KonanTarget.IOS_SIMULATOR_ARM64,
                        )
                    )
            }
        }

        listOf("linuxMain", "linuxTest").forEach { sourceSetName ->
            getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies(defaultBuildOptions.konanDataDir!!)
                .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                .assertTargetOnAllDependencies(
                    CommonizerTarget(KonanTarget.LINUX_ARM64, KonanTarget.LINUX_X64)
                )
        }
    }

    private fun WithSourceSetCommonizerDependencies.assertP3SourceSetDependencies() {
        /*
        windowsAndLinuxMain / windowsAndLinuxTest will not have a 'perfect target match' in p1.
        They will choose cinterops associated with 'nativeMain'
        */
        listOf("nativeMain", "nativeTest", "windowsAndLinuxMain", "windowsAndLinuxTest").forEach { sourceSetName ->
            getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies(defaultBuildOptions.konanDataDir!!)
                .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                .assertTargetOnAllDependencies(
                    CommonizerTarget(
                        KonanTarget.LINUX_ARM64,
                        KonanTarget.LINUX_X64,
                        KonanTarget.IOS_ARM64,
                        KonanTarget.IOS_X64,
                        KonanTarget.IOS_SIMULATOR_ARM64,
                        KonanTarget.MACOS_X64,
                        KonanTarget.MINGW_X64
                    )
                )
        }

        if (HostManager.hostIsMac) {
            listOf("appleAndLinuxMain", "appleAndLinuxTest").forEach { sourceSetName ->
                getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies(defaultBuildOptions.konanDataDir!!)
                    .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                    .assertTargetOnAllDependencies(
                        CommonizerTarget(
                            KonanTarget.LINUX_ARM64,
                            KonanTarget.LINUX_X64,
                            KonanTarget.IOS_ARM64,
                            KonanTarget.IOS_X64,
                            KonanTarget.IOS_SIMULATOR_ARM64,
                            KonanTarget.MACOS_X64,
                        )
                    )
            }

            listOf("iosMain", "iosTest").forEach { sourceSetName ->
                getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies(defaultBuildOptions.konanDataDir!!)
                    .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                    .assertTargetOnAllDependencies(
                        CommonizerTarget(
                            KonanTarget.IOS_ARM64,
                            KonanTarget.IOS_X64,
                            KonanTarget.IOS_SIMULATOR_ARM64
                        )
                    )
            }
        }
    }

    @DisplayName("UP-TO-DATE transformations for P3 subproject")
    @GradleTest
    fun transformationsUpToDateOnP3(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path,
    ) {
        project(cinteropProjectName, gradleVersion, localRepoDir = localRepo) {
            publishP1ToBuildRepository()

            build(":p3:transformNativeMainCInteropDependenciesMetadata", repositoryDependencyMode) {
                assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
            }

            build(":p3:transformNativeMainCInteropDependenciesMetadata", repositoryDependencyMode) {
                assertTasksUpToDate(":p3:transformNativeMainCInteropDependenciesMetadata")
            }

            val p3BuildGradleKts = subProject("p3").buildGradleKts
            val p3BuildGradleKtsContent = p3BuildGradleKts.readText()

            // Remove dependency on p2 | Task should re-run
            p3BuildGradleKts.writeText(
                p3BuildGradleKtsContent.replace("""implementation(project(":p2"))""", "")
            )
            build(":p3:transformNativeMainCInteropDependenciesMetadata", repositoryDependencyMode) {
                assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
            }

            // Re-add dependency on p3 | Task should re-run for the next invocation
            p3BuildGradleKts.writeText(p3BuildGradleKtsContent)
            build(":p3:transformNativeMainCInteropDependenciesMetadata", repositoryDependencyMode) {
                assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
            }

            build(":p3:transformNativeMainCInteropDependenciesMetadata", repositoryDependencyMode) {
                assertTasksUpToDate(":p3:transformNativeMainCInteropDependenciesMetadata")
            }

            // Replace dependency to :p2 with coordinates directly
            p3BuildGradleKts.writeText(
                p3BuildGradleKtsContent.replace("""project(":p2")""", """"kotlin-multiplatform-projects:p1:1.0.0-SNAPSHOT"""")
            )
            build(":p3:transformNativeMainCInteropDependenciesMetadata", repositoryDependencyMode) {
                /* Same binaries to transform; but project(":p2") is excluded from Task Inputs now */
                assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
            }
        }
    }

    @DisplayName("UP-TO-DATE transformations on adding/removing targets in repositories mode")
    @GradleTest
    fun upToDateTransformationsAddingRemovingTargetRepositoriesMode(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path,
    ) {
        project(cinteropProjectName, gradleVersion, localRepoDir = localRepo) {
            publishP1ToBuildRepository()
            testUpToDateTransformationOnRemovingOrAddingTargets(repositoryDependencyMode)
        }
    }

    @DisplayName("UP-TO-DATE transformations on adding/removing targets in project mode")
    @GradleTest
    fun upToDateTransformationsAddingRemovingTargetRepositoriesMode(gradleVersion: GradleVersion) {
        project(cinteropProjectName, gradleVersion) {
            testUpToDateTransformationOnRemovingOrAddingTargets(projectDependencyMode)
        }
    }

    private fun TestProject.testUpToDateTransformationOnRemovingOrAddingTargets(
        dependencyMode: String,
    ) {
        build(":p3:transformNativeMainCInteropDependenciesMetadata", dependencyMode)

        build(":p3:transformNativeMainCInteropDependenciesMetadata", dependencyMode) {
            assertTasksUpToDate(":p3:transformNativeMainCInteropDependenciesMetadata")
        }
    }

    private val cinteropProjectNameForKt50952 = "cinterop-MetadataDependencyTransformation-kt-50952"

    @DisplayName("KT-50952: UP-TO-DATE on changing consumer targets in repository mode")
    @GradleTest
    fun kt50952UpToDateChangingConsumerTargetsRepositoryMode(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path,
    ) {
        project(cinteropProjectNameForKt50952, gradleVersion, localRepoDir = localRepo) {
            publishP1ToBuildRepository()
            testUpToDateOnChangingConsumerTargets(repositoryDependencyMode)
        }
    }

    @DisplayName("KT-50952: UP-TO-DATE on changing consumer targets in project mode")
    @GradleTest
    fun kt50952UpToDateChangingConsumerTargetsRepositoryMode(gradleVersion: GradleVersion) {
        project(cinteropProjectNameForKt50952, gradleVersion) {
            testUpToDateOnChangingConsumerTargets(projectDependencyMode)
        }
    }

    private fun TestProject.testUpToDateOnChangingConsumerTargets(
        dependencyMode: String,
    ) {
        build(":p2:transformCommonMainCInteropDependenciesMetadata", dependencyMode)

        build(":p2:transformCommonMainCInteropDependenciesMetadata", dependencyMode) {
            assertTasksUpToDate(":p2:transformCommonMainCInteropDependenciesMetadata")
        }

        val optionToEnableAdditionalTarget = "-Pp2.enableAdditionalTarget"

        build(
            ":p2:transformCommonMainCInteropDependenciesMetadata",
            optionToEnableAdditionalTarget,
            dependencyMode
        ) {
            assertTasksExecuted(":p2:transformCommonMainCInteropDependenciesMetadata")
        }

        build(
            ":p2:transformCommonMainCInteropDependenciesMetadata",
            optionToEnableAdditionalTarget,
            dependencyMode
        ) {
            assertTasksUpToDate(":p2:transformCommonMainCInteropDependenciesMetadata")
        }

        build(":p2:transformCommonMainCInteropDependenciesMetadata", dependencyMode) {
            assertTasksExecuted(":p2:transformCommonMainCInteropDependenciesMetadata")
        }

        build(":p2:transformCommonMainCInteropDependenciesMetadata", dependencyMode) {
            assertTasksUpToDate(":p2:transformCommonMainCInteropDependenciesMetadata")
        }
    }

    @DisplayName("KT-71328: no tasks instantiated at execution time during CInterop GMT")
    @TestMetadata("kt-71328")
    @GradleTest
    fun testNoTasksInstantiatedAtExecutionTimeCinteropGmt(gradleVersion: GradleVersion) {
        // configuration cache may hide the problem,
        // especially from Gradle 8.0 as it started to serialize the state even before the first execution
        // so disabling it in this test is mandatory
        val buildOptions = defaultBuildOptions.copy(configurationCache = ConfigurationCacheValue.DISABLED)
        project("kt-71328", gradleVersion, buildOptions = buildOptions) {
            val projectsToApply = listOf(this, subProject("lib"))
            for (testProject in projectsToApply) {
                testProject.buildScriptInjection {
                    TaskInstantiationTrackingBuildService.trackInstantiationInProject(project)
                }
            }
            build(":transformNativeMainCInteropDependenciesMetadata")
        }
    }
}

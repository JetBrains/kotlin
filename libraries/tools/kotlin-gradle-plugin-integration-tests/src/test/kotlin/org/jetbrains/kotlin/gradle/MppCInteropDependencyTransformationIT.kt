/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.gradle.util.WithSourceSetCommonizerDependencies
import org.jetbrains.kotlin.gradle.util.reportSourceSetCommonizerDependencies
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.junit.Test

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
class MppCInteropDependencyTransformationIT : BaseGradleIT() {

    private val baseBuildOptions = defaultBuildOptions().copy(
        forceOutputToStdout = true,
        warningMode = WarningMode.Fail,
        parallelTasksInProject = true,
        freeCommandLineArgs = defaultBuildOptions().freeCommandLineArgs + "-s"
    )

    private val projectDependencyOptions = baseBuildOptions.copy(
        freeCommandLineArgs = baseBuildOptions.freeCommandLineArgs + "-PdependencyMode=project"
    )

    private val repositoryDependencyOptions = baseBuildOptions.copy(
        freeCommandLineArgs = baseBuildOptions.freeCommandLineArgs + "-PdependencyMode=repository"
    )

    private val project by lazy { Project("cinterop-MetadataDependencyTransformation") }

    @Test
    fun `test - compile project - dependencyMode=project`() {
        testCompileProject(projectDependencyOptions) {
            assertProjectDependencyMode()
            assertTasksExecuted(":p1:commonizeCInterop")
        }
    }

    @Test
    fun `test - compile project - dependencyMode=repository`() {
        publishP1ToBuildRepository()
        testCompileProject(repositoryDependencyOptions) {
            assertRepositoryDependencyMode()
        }
    }

    private fun testCompileProject(options: BuildOptions, check: CompiledProject.() -> Unit = {}) {
        project.build("compileAll", options = options) {
            check()
            assertSuccessful()

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

    @Test
    fun `test - source set dependencies - dependencyMode=project`() {
        reportSourceSetCommonizerDependencies(project, "p2", projectDependencyOptions) {
            it.assertProjectDependencyMode()
            it.assertTasksExecuted(":p2:transformNativeMainCInteropDependenciesMetadataForIde")
            it.assertTasksNotExecuted(".*[cC]ompile.*")
            assertP2SourceSetDependencies()
        }

        reportSourceSetCommonizerDependencies(project, "p3", projectDependencyOptions) {
            it.assertProjectDependencyMode()
            it.assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadataForIde")
            it.assertTasksNotExecuted(".*[cC]ompile.*")
            assertP3SourceSetDependencies()
        }
    }

    @Test
    fun `test - source set dependencies - dependencyMode=repository`() {
        publishP1ToBuildRepository()

        reportSourceSetCommonizerDependencies(project, "p2", repositoryDependencyOptions) {
            it.assertRepositoryDependencyMode()
            it.assertTasksExecuted(":p2:transformNativeMainCInteropDependenciesMetadataForIde")
            it.assertTasksNotExecuted(".*[cC]ompile.*")
            assertP2SourceSetDependencies()
        }

        reportSourceSetCommonizerDependencies(project, "p3", repositoryDependencyOptions) {
            it.assertRepositoryDependencyMode()
            it.assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadataForIde")
            it.assertTasksNotExecuted(".*[cC]ompile.*")
            assertP3SourceSetDependencies()
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

    @Test
    fun `test - transformation - UP-TO-DATE behaviour`() {
        publishP1ToBuildRepository()
        project.build(":p3:transformNativeMainCInteropDependenciesMetadata", options = repositoryDependencyOptions) {
            assertSuccessful()
            assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
        }

        project.build(":p3:transformNativeMainCInteropDependenciesMetadata", options = repositoryDependencyOptions) {
            assertSuccessful()
            assertTasksNotExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
            assertTasksUpToDate(":p3:transformNativeMainCInteropDependenciesMetadata")
        }

        val p3BuildGradleKts = project.projectDir.resolve("p3/build.gradle.kts")
        val p3BuildGradleKtsContent = p3BuildGradleKts.readText()

        // Remove dependency on p2 | Task should re-run
        p3BuildGradleKts.writeText(p3BuildGradleKtsContent.replace("""implementation(project(":p2"))""", ""))
        project.build(":p3:transformNativeMainCInteropDependenciesMetadata", options = repositoryDependencyOptions) {
            assertSuccessful()
            assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
        }

        // Re-add dependency on p3 | Task should re-run for the next invocation
        p3BuildGradleKts.writeText(p3BuildGradleKtsContent)
        project.build(":p3:transformNativeMainCInteropDependenciesMetadata", options = repositoryDependencyOptions) {
            assertSuccessful()
            assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
        }
        project.build(":p3:transformNativeMainCInteropDependenciesMetadata", options = repositoryDependencyOptions) {
            assertSuccessful()
            assertTasksNotExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
            assertTasksUpToDate(":p3:transformNativeMainCInteropDependenciesMetadata")
        }

        // Replace dependency to :p2 with coordinates directly
        p3BuildGradleKts.writeText(
            p3BuildGradleKtsContent.replace("""project(":p2")""", """"kotlin-multiplatform-projects:p1:1.0.0-SNAPSHOT"""")
        )
        project.build(":p3:transformNativeMainCInteropDependenciesMetadata", options = repositoryDependencyOptions) {
            assertSuccessful()
            assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
        }
    }

    private fun publishP1ToBuildRepository() {
        project.build(":p1:publishAllPublicationsToBuildRepository", options = repositoryDependencyOptions) {
            assertSuccessful()
        }
    }

    private fun CompiledProject.assertProjectDependencyMode() {
        assertContains("dependencyMode = 'project'")
    }

    private fun CompiledProject.assertRepositoryDependencyMode() {
        assertContains("dependencyMode = 'repository'")
    }
}

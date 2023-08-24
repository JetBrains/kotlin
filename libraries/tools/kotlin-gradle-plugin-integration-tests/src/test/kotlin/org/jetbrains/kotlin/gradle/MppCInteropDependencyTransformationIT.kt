/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

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
abstract class MppCInteropDependencyTransformationIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.FOR_MPP_SUPPORT

    override fun defaultBuildOptions(): BuildOptions = super.defaultBuildOptions().run {
        copy(
            forceOutputToStdout = true,
            parallelTasksInProject = true,
            freeCommandLineArgs = freeCommandLineArgs + "-s"
        )
    }

    protected val projectDependencyOptions
        get() = defaultBuildOptions().copy(
            freeCommandLineArgs = defaultBuildOptions().freeCommandLineArgs + "-PdependencyMode=project"
        )

    protected val repositoryDependencyOptions
        get() = defaultBuildOptions().copy(
            freeCommandLineArgs = defaultBuildOptions().freeCommandLineArgs + "-PdependencyMode=repository"
        )

    class ComplexProject : MppCInteropDependencyTransformationIT() {

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

                /* Assert p2 & p3 transformed cinterop dependencies */
                assertTasksExecuted(":p2:transformNativeMainCInteropDependenciesMetadata")
                assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")

                /* Assert p2 & p3 compiled for Windows */
                assertTasksExecuted(":p2:compileKotlinWindowsX64")
                assertTasksExecuted(":p3:compileKotlinWindowsX64")

                /* Assert p2 & p3 compiled tests */
                assertTasksExecuted(":p2:compileTestKotlinLinuxX64")
                assertTasksExecuted(":p3:compileTestKotlinLinuxX64")

                /* Configurations should not be resolved during configuration phase */
                assertNotContains("Configuration resolved before Task Graph is ready")
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
                        CommonizerTarget(LINUX_ARM64, LINUX_X64, IOS_ARM64, IOS_X64, MACOS_X64, MINGW_X64)
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

            listOf("linuxMain", "linuxTest").forEach { sourceSetName ->
                getCommonizerDependencies(sourceSetName).withoutNativeDistributionDependencies()
                    .assertDependencyFilesMatches(".*cinterop-simple.*", ".*cinterop-withPosix.*")
                    .assertTargetOnAllDependencies(CommonizerTarget(LINUX_ARM64, LINUX_X64))
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
                        CommonizerTarget(LINUX_ARM64, LINUX_X64, IOS_ARM64, IOS_X64, MACOS_X64, MINGW_X64)
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
        }

        @Test
        fun `test - transformation - UP-TO-DATE behaviour - on p3`() {
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
                /* Same binaries to transform; but project(":p2") is excluded from Task Inputs now */
                assertTasksExecuted(":p3:transformNativeMainCInteropDependenciesMetadata")
            }
        }


        @Test
        fun `test - transformation - UP-TO-DATE behaviour - on removing and adding targets - on p2 - dependencyMode=repository`() {
            publishP1ToBuildRepository()
            `test - transformation - UP-TO-DATE behaviour - on removing and adding targets - on p2`(repositoryDependencyOptions)
        }

        @Test
        fun `test - transformation - UP-TO-DATE behaviour - on removing and adding targets - on p2 - dependencyMode=project`() {
            `test - transformation - UP-TO-DATE behaviour - on removing and adding targets - on p2`(projectDependencyOptions)
        }

        private fun `test - transformation - UP-TO-DATE behaviour - on removing and adding targets - on p2`(options: BuildOptions) {
            project.build(":p3:transformNativeMainCInteropDependenciesMetadata", options = options) {
                assertSuccessful()
            }

            project.build(":p3:transformNativeMainCInteropDependenciesMetadata", options = options) {
                assertTasksUpToDate(":p3:transformNativeMainCInteropDependenciesMetadata")
            }
        }

        private fun publishP1ToBuildRepository() = project.publishP1ToBuildRepository()
    }

    class KT50952 : MppCInteropDependencyTransformationIT() {
        private val project by lazy { Project("cinterop-MetadataDependencyTransformation-kt-50952") }

        @Test
        fun `test UP-TO-DATE - when changing consumer targets - dependencyMode=repository`() {
            project.publishP1ToBuildRepository()
            `test UP-TO-DATE - when changing consumer targets`(repositoryDependencyOptions)
        }

        @Test
        fun `test UP-TO-DATE - when changing consumer targets - dependencyMode=project`() {
            `test UP-TO-DATE - when changing consumer targets`(projectDependencyOptions)
        }

        private fun `test UP-TO-DATE - when changing consumer targets`(options: BuildOptions) {
            project.build(":p2:transformCommonMainCInteropDependenciesMetadata", options = options) {
                assertSuccessful()
            }

            project.build(":p2:transformCommonMainCInteropDependenciesMetadata", options = options) {
                assertSuccessful()
                assertTasksNotExecuted(":p2:transformCommonMainCInteropDependenciesMetadata")
                assertTasksUpToDate(":p2:transformCommonMainCInteropDependenciesMetadata")
            }

            val optionsWithAdditionalTargetEnabled = options.withFreeCommandLineArgument("-Pp2.enableAdditionalTarget")

            project.build(":p2:transformCommonMainCInteropDependenciesMetadata", options = optionsWithAdditionalTargetEnabled) {
                assertSuccessful()
                assertTasksExecuted(":p2:transformCommonMainCInteropDependenciesMetadata")
            }

            project.build(":p2:transformCommonMainCInteropDependenciesMetadata", options = optionsWithAdditionalTargetEnabled) {
                assertSuccessful()
                assertTasksNotExecuted(":p2:transformCommonMainCInteropDependenciesMetadata")
                assertTasksUpToDate(":p2:transformCommonMainCInteropDependenciesMetadata")
            }

            project.build(":p2:transformCommonMainCInteropDependenciesMetadata", options = options) {
                assertSuccessful()
                assertTasksExecuted(":p2:transformCommonMainCInteropDependenciesMetadata")
            }

            project.build(":p2:transformCommonMainCInteropDependenciesMetadata", options = options) {
                assertSuccessful()
                assertTasksNotExecuted(":p2:transformCommonMainCInteropDependenciesMetadata")
                assertTasksUpToDate(":p2:transformCommonMainCInteropDependenciesMetadata")
            }
        }
    }

    protected fun CompiledProject.assertProjectDependencyMode() {
        assertContains("dependencyMode = 'project'")
    }

    protected fun CompiledProject.assertRepositoryDependencyMode() {
        assertContains("dependencyMode = 'repository'")
    }

    protected fun Project.publishP1ToBuildRepository() {
        build(":p1:publishAllPublicationsToBuildRepository", options = repositoryDependencyOptions) {
            assertSuccessful()
        }
    }
}

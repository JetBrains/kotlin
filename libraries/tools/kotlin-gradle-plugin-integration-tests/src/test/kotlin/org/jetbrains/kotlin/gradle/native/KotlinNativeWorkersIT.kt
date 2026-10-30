/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.native.KotlinNativeToolParallelismTestProbeService.Companion.observedMaxParallelWorkers
import org.jetbrains.kotlin.gradle.native.KotlinNativeToolParallelismTestProbeService.Companion.registerParallelismProbeServiceIfAbsent
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.ConfigurationCacheValue
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.IsolatedProjectsMode
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.NativeGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.compileStubSourceWithSourceSetName
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.test.assertEquals

@DisplayName("Smoke tests for Kotlin Native tasks that use Workers API")
@NativeGradlePluginTests
class KotlinNativeWorkersIT : KGPBaseTest() {

    @DisplayName("Native compile, link and cinterop commonizer tasks run in parallel up to their maxWorkers limit with configuration cache disabled")
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    fun testNativeTaskParallelismIsCappedWithConfigurationCacheDisabled(gradleVersion: GradleVersion) =
        assertNativeTaskParallelismIsCapped(gradleVersion, IsolatedProjectsMode.DISABLED,ConfigurationCacheValue.DISABLED)

    @DisplayName("Native compile, link and cinterop commonizer tasks run in parallel up to their maxWorkers limit with configuration cache enabled")
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    fun testNativeTaskParallelismIsCappedWithConfigurationCacheEnabled(gradleVersion: GradleVersion) =
        assertNativeTaskParallelismIsCapped(gradleVersion, IsolatedProjectsMode.ENABLED, ConfigurationCacheValue.ENABLED)

    private fun assertNativeTaskParallelismIsCapped(
        gradleVersion: GradleVersion,
        isolatedProjectsMode: IsolatedProjectsMode,
        configurationCache: ConfigurationCacheValue,
    ) {
        val compileMaxWorkers = 2
        val linkMaxWorkers = 2
        val commonizeCInteropMaxWorkers = 2
        val buildOptions = defaultBuildOptions.copy(
            configurationCache = configurationCache,
            isolatedProjects = isolatedProjectsMode,
            maxWorkers = compileMaxWorkers + linkMaxWorkers + commonizeCInteropMaxWorkers + 1,
        )
        project("empty", gradleVersion, buildOptions) {
            plugins {
                kotlin("multiplatform")
            }
            val reportDir = projectPath.resolve("build/parallelismReports").toFile()
            buildScriptInjection {
                val defFile = project.layout.projectDirectory.dir("src/nativeInterop/cinterop").file("myInterop.def").asFile
                defFile.parentFile.mkdirs()
                defFile.writeText("language = Objective-C")

                project.applyMultiplatform {
                    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
                        it.binaries.framework { baseName = "libFramework" }
                        it.compilations.getByName("main").cinterops.create("myInterop") { interop ->
                            interop.definitionFile.set(defFile)
                        }
                    }
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }

                project.registerParallelismProbeServiceIfAbsent<KotlinNativeCompile>(reportDir, expectedRunning = compileMaxWorkers)
                project.registerParallelismProbeServiceIfAbsent<KotlinNativeLink>(reportDir, expectedRunning = linkMaxWorkers)
                project.registerParallelismProbeServiceIfAbsent<CInteropCommonizerTask>(reportDir, expectedRunning = commonizeCInteropMaxWorkers)
            }

            listOf("lib1", "lib2").forEach { name ->
                val subproject = project("empty", gradleVersion) {
                    buildScriptInjection {
                        val defFile = project.layout.projectDirectory.dir("src/nativeInterop/cinterop").file("myInterop.def").asFile
                        defFile.parentFile.mkdirs()
                        defFile.writeText("language = Objective-C")

                        project.applyMultiplatform {
                            listOf(iosX64(), iosArm64()).forEach {
                                it.compilations.getByName("main").cinterops.create("myInterop") { interop ->
                                    interop.definitionFile.set(defFile)
                                }
                            }
                            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                        }

                        project.registerParallelismProbeServiceIfAbsent<KotlinNativeCompile>(reportDir, expectedRunning = compileMaxWorkers)
                        project.registerParallelismProbeServiceIfAbsent<CInteropCommonizerTask>(reportDir, expectedRunning = commonizeCInteropMaxWorkers)
                    }
                }
                include(subproject, name)
            }

            build(
                "assemble",
                "-Pkotlin.mpp.enableCInteropCommonization=true",
                "-Pkotlin.native.compileMaxWorkers=$compileMaxWorkers",
                "-Pkotlin.native.linkMaxWorkers=$linkMaxWorkers",
                "-Pkotlin.native.commonizeCInteropMaxWorkers=$commonizeCInteropMaxWorkers",
            ) {
                assertTasksExecuted(
                    ":compileKotlinIosX64",
                    ":compileKotlinIosArm64",
                    ":compileKotlinIosSimulatorArm64",
                    ":lib1:compileKotlinIosX64",
                    ":lib1:compileKotlinIosArm64",
                    ":lib2:compileKotlinIosX64",
                    ":lib2:compileKotlinIosArm64",
                    ":linkDebugFrameworkIosX64",
                    ":linkDebugFrameworkIosArm64",
                    ":linkDebugFrameworkIosSimulatorArm64",
                    ":linkReleaseFrameworkIosX64",
                    ":linkReleaseFrameworkIosArm64",
                    ":linkReleaseFrameworkIosSimulatorArm64",
                    ":commonizeCInterop",
                    ":lib1:commonizeCInterop",
                    ":lib2:commonizeCInterop",
                )
            }

            // Any 2 of compileKotlinIosX64/IosArm64/IosSimulatorArm64 in root, compileKotlinIosX64/IosArm64 in :lib1 and :lib2.
            assertEquals(compileMaxWorkers, reportDir.observedMaxParallelWorkers<KotlinNativeCompile>())
            // Any 2 of root link{Debug,Release}FrameworkIos{X64,Arm64,SimulatorArm64}.
            assertEquals(linkMaxWorkers, reportDir.observedMaxParallelWorkers<KotlinNativeLink>())
            // Any 2 of :commonizeCInterop, :lib1:commonizeCInterop, :lib2:commonizeCInterop.
            assertEquals(commonizeCInteropMaxWorkers, reportDir.observedMaxParallelWorkers<CInteropCommonizerTask>())
        }
    }
}

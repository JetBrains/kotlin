/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTask
import org.jetbrains.kotlin.gradle.targets.native.tasks.artifact.KotlinNativeLinkArtifactTask
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assume
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class XCFrameworkKotlinArtifactsTest {

    @BeforeTest
    fun runOnMacOSOnly() {
        Assume.assumeTrue(HostManager.hostIsMac)
    }

    @Test
    fun `xcframework task graph - with universal and regular frameworks`() {
        val project = buildProjectWithMPP {
            kotlin { linuxArm64() }
            kotlinArtifacts {
                Native.XCFramework { xcframeworkConfig ->
                    xcframeworkConfig.targets(
                        KonanTarget.IOS_ARM64,
                        KonanTarget.IOS_SIMULATOR_ARM64,
                        KonanTarget.IOS_X64,
                    )
                }
            }
        }.evaluate()

        val xcframeworkTask = assertIsInstance<XCFrameworkTask>(project.tasks.getByName("assembleTestReleaseXCFramework"))

        assertEquals(
            project.buildFile("out/xcframework"),
            xcframeworkTask.outputDir,
        )

        val universalFrameworkTask = assertIsInstance<FatFrameworkTask>(
            project.tasks.getByName("assembleReleaseIosSimulatorFatFrameworkForTestXCFramework")
        )
        val thinFrameworkIosArm64Task = assertIsInstance<KotlinNativeLinkArtifactTask>(
            project.tasks.getByName("assembleTestReleaseFrameworkIosArm64ForXCF")
        )

        assert(
            xcframeworkTask.taskDependencies.getDependencies(null).containsAll(
                setOf(
                    universalFrameworkTask,
                    thinFrameworkIosArm64Task,
                )
            )
        )

        assertEquals(
            listOf(
                project.buildFile("testXCFrameworkTemp/ios_arm64/release/test.framework"),
                project.buildFile("testXCFrameworkTemp/fatframework/release/iosSimulator/test.framework"),
            ),
            xcframeworkTask.xcframeworkSlices().map { it.file },
        )
        assertEquals(
            project.buildFile("testXCFrameworkTemp/ios_arm64/release/test.framework"),
            thinFrameworkIosArm64Task.outputFile.get()
        )

        val thinFrameworkIosSimulatorArm64Task = assertIsInstance<KotlinNativeLinkArtifactTask>(
            project.tasks.getByName("assembleTestReleaseFrameworkIosSimulatorArm64ForXCF")
        )
        val thinFrameworkIosX64Task = assertIsInstance<KotlinNativeLinkArtifactTask>(
            project.tasks.getByName("assembleTestReleaseFrameworkIosX64ForXCF")
        )
        assertEquals(
            setOf(
                thinFrameworkIosSimulatorArm64Task,
                thinFrameworkIosX64Task,
            ),
            universalFrameworkTask.taskDependencies.getDependencies(null)
        )
        assertEquals(
            listOf(
                project.buildFile("testXCFrameworkTemp/ios_simulator_arm64/release/test.framework"),
                project.buildFile("testXCFrameworkTemp/ios_x64/release/test.framework"),
            ),
            universalFrameworkTask.frameworks.map { it.file },
        )
    }

    @Test
    fun `parent task dependency - is created`() {
        val project = buildProjectWithMPP {
            kotlin { linuxArm64() }
            kotlinArtifacts {
                Native.XCFramework { xcframeworkConfig ->
                    xcframeworkConfig.targets(
                        KonanTarget.IOS_ARM64,
                        KonanTarget.IOS_SIMULATOR_ARM64,
                        KonanTarget.IOS_X64,
                    )
                }
            }
        }.evaluate()

        val parentTask = project.tasks.getByName("assembleTestXCFramework")

        assertEquals(
            setOf(
                project.tasks.named("assembleTestDebugXCFramework"),
                project.tasks.named("assembleTestReleaseXCFramework"),
            ),
            parentTask.dependsOn,
        )
    }

    private fun Project.buildFile(path: String) = layout.buildDirectory.file(path).get().asFile

}
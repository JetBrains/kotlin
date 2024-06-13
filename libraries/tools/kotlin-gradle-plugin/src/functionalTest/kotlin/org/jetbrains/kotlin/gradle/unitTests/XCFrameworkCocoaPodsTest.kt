/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.PodspecTask
import org.jetbrains.kotlin.gradle.util.applyCocoapodsPlugin
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.cocoapods
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class XCFrameworkCocoaPodsTest {

    @BeforeTest
    fun runOnMacOSOnly() {
        Assume.assumeTrue(HostManager.hostIsMac)
    }

    @Test
    fun `expected xcframework input slices - in cocoapods generated xcframework`() {
        val project = buildProjectWithMPP {
            applyCocoapodsPlugin()
            kotlin {
                listOf(
                    iosSimulatorArm64(),
                    iosX64(),
                    iosArm64(),
                )

                cocoapods {
                    framework {
                        baseName = "foo"
                    }
                }
            }
        }.evaluate()

        val xcframeworkTask = project.tasks.getByName("podPublishReleaseXCFramework") as XCFrameworkTask

        assertEquals(
            setOf(
                project.buildFile("bin/iosArm64/podReleaseFramework/foo.framework"),
                project.buildFile("fooXCFrameworkTemp/fatframework/release/iosSimulator/foo.framework"),
            ),
            xcframeworkTask.xcframeworkSlices().map { it.file }.toSet()
        )
    }

    @Test
    fun `parent task dependency - is created - for cocoapods xcframework`() {
        val project = buildProjectWithMPP {
            kotlin {
                iosSimulatorArm64()
            }
            applyCocoapodsPlugin()
        }.evaluate()

        val parentTask = project.tasks.getByName("podPublishXCFramework")

        assertEquals(
            setOf(
                project.tasks.named("podPublishDebugXCFramework").get(),
                project.tasks.named("podPublishReleaseXCFramework").get(),
            ),
            parentTask.taskDependencies.getDependencies(null),
        )
    }

    @Test
    fun `cocoapods podspec publication - depends on xcframework task`() {
        val project = buildProjectWithMPP {
            kotlin {
                iosSimulatorArm64()
            }
            applyCocoapodsPlugin()
        }.evaluate()

        assertEquals(
            listOf(project.tasks.named("podSpecRelease")),
            project.tasks.getByName("podPublishReleaseXCFramework").dependsOn.filter {
                it is TaskProvider<*> && it.get() is PodspecTask
            }
        )
    }

    private fun Project.buildFile(path: String) = layout.buildDirectory.file(path).get().asFile

}
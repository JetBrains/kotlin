/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.FrameworkDescriptor
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.util.assertThrows
import org.junit.Assume
import java.io.File
import kotlin.test.*

class XCFrameworkTaskTest {

    @BeforeTest
    fun runOnMacOSOnly() {
        Assume.assumeTrue(HostManager.hostIsMac)
    }

    @Test
    fun `expected xcframework input slices - when exporting a single framework`() {
        val project = buildProjectWithMPP {
            val xcframework = XCFramework()
            kotlin {
                iosSimulatorArm64().binaries.framework {
                    xcframework.add(this)
                }
            }
        }.evaluate()

        val xcframeworkTask = assertIsInstance<XCFrameworkTask>(project.tasks.getByName("assembleTestReleaseXCFramework"))

        xcframeworkTask.assertDependsOn(
            project.tasks.getByName("linkReleaseFrameworkIosSimulatorArm64")
        )
        assertEquals(
            listOf(
                project.buildFile("bin/iosSimulatorArm64/releaseFramework/test.framework")
            ),
            xcframeworkTask.xcframeworkSlices().map { it.file }
        )
    }

    @Test
    fun `expected xcframework input slices - when exporting multiple frameworks`() {
        val project = buildProjectWithMPP {
            val xcframework = XCFramework()
            kotlin {
                listOf(
                    iosSimulatorArm64(),
                    iosArm64(),
                ).forEach {
                    it.binaries.framework {
                        xcframework.add(this)
                    }
                }
            }
        }.evaluate()

        val xcframeworkTask = assertIsInstance<XCFrameworkTask>(project.tasks.getByName("assembleTestReleaseXCFramework"))

        assertEquals(
            listOf(
                project.buildFile("bin/iosSimulatorArm64/releaseFramework/test.framework"),
                project.buildFile("bin/iosArm64/releaseFramework/test.framework"),
            ),
            xcframeworkTask.xcframeworkSlices().map { it.file }
        )
    }

    @Test
    fun `expected xcframework input slices - contain only universal framework - when multiple frameworks must be merged`() {
        val project = buildProjectWithMPP {
            val xcframework = XCFramework()
            kotlin {
                listOf(
                    iosSimulatorArm64(),
                    iosX64(),
                    iosArm64(),
                ).forEach {
                    it.binaries.framework {
                        xcframework.add(this)
                    }
                }
            }
        }.evaluate()

        val xcframeworkTask = project.tasks.getByName("assembleTestReleaseXCFramework") as XCFrameworkTask

        assertEquals(
            listOf(
                project.buildFile("testXCFrameworkTemp/fatframework/release/iosSimulator/test.framework"),
                project.buildFile("bin/iosArm64/releaseFramework/test.framework"),
            ),
            xcframeworkTask.xcframeworkSlices().map { it.file }
        )
    }

    @Test
    fun `universal framework dependency - with multiple universal frameworks`() {
        val project = buildProjectWithMPP {
            val xcframework = XCFramework()
            kotlin {
                listOf(
                    iosSimulatorArm64(),
                    iosX64(),
                    watchosArm64(),
                    watchosDeviceArm64(),
                    watchosArm32(),
                ).forEach {
                    it.binaries.framework {
                        xcframework.add(this)
                    }
                }
            }
        }.evaluate()

        val xcframeworkTask = assertIsInstance<XCFrameworkTask>(project.tasks.getByName("assembleTestReleaseXCFramework"))

        assertEquals(
            listOf(
                project.buildFile("testXCFrameworkTemp/fatframework/release/iosSimulator/test.framework"),
                project.buildFile("testXCFrameworkTemp/fatframework/release/watchos/test.framework"),
            ),
            xcframeworkTask.xcframeworkSlices().map { it.file }
        )

        val universalFrameworkTasks = xcframeworkTask.taskDependencies.getDependencies(null)
            .filterIsInstance<FatFrameworkTask>()
            .filter { it.frameworks.isNotEmpty() }

        assertEquals(2, universalFrameworkTasks.size)

        val watchosUniversalFrameworkTask = universalFrameworkTasks.single { it.frameworks.first().target.family == Family.WATCHOS }
        val iosUniversalFrameworkTask = universalFrameworkTasks.single { it.frameworks.first().target.family == Family.IOS }

        assertEquals(
            setOf(
                project.multiplatformExtension.iosSimulatorArm64().binaries.getFramework(NativeBuildType.RELEASE).linkTaskProvider.get(),
                project.multiplatformExtension.iosX64().binaries.getFramework(NativeBuildType.RELEASE).linkTaskProvider.get(),
            ),
            iosUniversalFrameworkTask.taskDependencies.getDependencies(null)
        )
        assertEquals(
            listOf(
                project.buildFile("bin/iosSimulatorArm64/releaseFramework/test.framework"),
                project.buildFile("bin/iosX64/releaseFramework/test.framework"),
            ),
            iosUniversalFrameworkTask.frameworks.map { it.file },
        )
        assertEquals(
            project.buildFile("testXCFrameworkTemp/fatframework/release/iosSimulator/test.framework"),
            iosUniversalFrameworkTask.fatFramework,
        )
        assertEquals(
            listOf(
                project.buildFile("bin/watchosArm64/releaseFramework/test.framework"),
                project.buildFile("bin/watchosDeviceArm64/releaseFramework/test.framework"),
                project.buildFile("bin/watchosArm32/releaseFramework/test.framework"),
            ),
            watchosUniversalFrameworkTask.frameworks.map { it.file },
        )
        assertEquals(
            project.buildFile("testXCFrameworkTemp/fatframework/release/watchos/test.framework"),
            watchosUniversalFrameworkTask.fatFramework,
        )
    }

    @Test
    fun `parent task dependency - is created`() {
        val project = buildProjectWithMPP {
            val xcframework = XCFramework()
            kotlin {
                iosSimulatorArm64().binaries.framework {
                    xcframework.add(this)
                }
            }
        }.evaluate()

        val parentTask = project.tasks.getByName("assembleXCFramework")

        assertEquals(
            setOf(
                project.tasks.named("assembleTestDebugXCFramework"),
                project.tasks.named("assembleTestReleaseXCFramework"),
            ),
            parentTask.dependsOn,
        )
    }

    @Test
    fun `xcodebuild call - with universal and regular framework - points to corrent frameworks and dSYMs`() {
        val project = buildProjectWithMPP {
            val xcframework = XCFramework()
            kotlin {
                listOf(
                    iosSimulatorArm64(),
                    iosX64(),
                    iosArm64(),
                ).forEach {
                    it.binaries.framework {
                        baseName = "bar"
                        xcframework.add(this)
                    }
                }
            }
        }.evaluate()

        assertEquals(
            listOf(
                "xcodebuild", "-create-xcframework",
                // FIXME: KT-66894, KT-65675
                "-framework", project.buildFile("testXCFrameworkTemp/fatframework/release/iosSimulator/test.framework").path,
                "-debug-symbols", project.buildFile("testXCFrameworkTemp/fatframework/release/iosSimulator/test.framework.dSYM").path,
                "-framework", project.buildFile("bin/iosArm64/releaseFramework/bar.framework").path,
                "-debug-symbols", project.buildFile("bin/iosArm64/releaseFramework/bar.framework.dSYM").path,
                "-output", project.buildFile("XCFrameworks/release/test.xcframework").path,
            ),
            assertIsInstance<XCFrameworkTask>(
                project.tasks.getByName("assembleTestReleaseXCFramework")
            ).xcodebuildArguments(
                // Assume dSYM was created
                fileExists = { true }
            )
        )
    }

    @Test
    fun `xcodebuild call - doesn't point to dSYMs - when framework is static`() {
        val project = buildProjectWithMPP {
            val xcframework = XCFramework()
            kotlin {
                listOf(
                    iosSimulatorArm64(),
                ).forEach {
                    it.binaries.framework {
                        baseName = "bar"
                        isStatic = true
                        xcframework.add(this)
                    }
                }
            }
        }.evaluate()

        val xcframeworkTask = assertIsInstance<XCFrameworkTask>(
            project.tasks.getByName("assembleTestReleaseXCFramework")
        )

        assertEquals(
            listOf(
                "xcodebuild", "-create-xcframework",
                "-framework", project.buildFile("bin/iosSimulatorArm64/releaseFramework/bar.framework").path,
                "-output", project.buildFile("XCFrameworks/release/test.xcframework").path,
            ),
            xcframeworkTask.xcodebuildArguments(
                // Assume dSYM was created
                fileExists = { true }
            )
        )
    }

    @Test
    fun `framework names are different diagnostic - when indepent frameworks have different names`() {
        val project = buildProjectWithMPP {
            val xcframework = XCFramework()
            kotlin {
                iosSimulatorArm64().binaries.framework {
                    baseName = "Test"
                    xcframework.add(this)
                }
                iosArm64().binaries.framework {
                    baseName = "test"
                    xcframework.add(this)
                }
            }
        }.evaluate()

        // Force XCFramework task to configure
        assertThrows<RuntimeException> {
            assertIsInstance<XCFrameworkTask>(
                project.tasks.getByName("assembleTestReleaseXCFramework")
            ).validateInputFrameworks()
        }
    }

    private fun Project.buildFile(path: String) = layout.buildDirectory.file(path).get().asFile

}

fun XCFrameworkTask.xcodebuildArguments(
    fileExists: (File) -> Boolean = { it.exists() }
) = xcodebuildArguments(
    frameworkFiles = xcframeworkSlices(xcFrameworkName.get()),
    output = outputXCFrameworkFile,
    fileExists = fileExists,
)

fun XCFrameworkTask.validateInputFrameworks() = validateInputFrameworks(xcFrameworkName.get())

fun XCFrameworkTask.xcframeworkSlices() = xcframeworkSlices(xcFrameworkName.get())
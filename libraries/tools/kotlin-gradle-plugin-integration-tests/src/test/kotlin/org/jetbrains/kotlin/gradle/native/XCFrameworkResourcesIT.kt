/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTask
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectories
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.io.File
import kotlin.io.path.*
import kotlin.test.assertEquals

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@NativeGradlePluginTests
@DisplayName("Test multiplatform resources publication embedded in XCFramework")
class XCFrameworkResourcesIT : KGPBaseTest() {

    /**
     * Tests the tasks related to XCFramework resources to ensure they are up-to-date across multiple build executions.
     * This includes validating that resource linking, assembling, and embedding tasks for XCFramework remain up-to-date
     * when no changes are introduced between successive builds.
     */
    @DisplayName("XCFramework resorces tasks up-to-date")
    @GradleTest
    fun testXCFrameworkResourcesUpToDate(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            configureForResources {
                listOf(
                    iosX64(),
                    iosArm64(),
                    iosSimulatorArm64(),
                )
            }

            build("assembleSharedDebugXCFramework") {
                assertTasksExecuted(
                    ":linkDebugFrameworkIosArm64",
                    ":linkDebugFrameworkIosSimulatorArm64",
                    ":linkDebugFrameworkIosX64"
                )

                assertTasksExecuted(
                    ":assembleDebugIosSimulatorFatFrameworkForSharedXCFramework",
                    ":assembleSharedDebugXCFramework"
                )
            }

            build("assembleSharedDebugXCFramework") {
                assertTasksUpToDate(
                    ":linkDebugFrameworkIosArm64",
                    ":linkDebugFrameworkIosSimulatorArm64",
                    ":linkDebugFrameworkIosX64"
                )

                assertTasksUpToDate(
                    ":assembleDebugIosSimulatorFatFrameworkForSharedXCFramework",
                    ":assembleSharedDebugXCFramework"
                )
            }
        }
    }

    /**
     * Tests the publication of multiplatform resources for multiple targets embedded
     * within an XCFramework.
     */
    @DisplayName("Multiplatform resources publication for multiple targets embedded in XCFramework")
    @GradleTest
    fun testXCFrameworkMultipleTargetsResourcesPublication(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            configureForResources {
                listOf(
                    iosX64(),
                    iosArm64(),
                    iosSimulatorArm64(),
                )
            }

            build("assembleSharedDebugXCFramework") {
                assertTasksExecuted(":assembleSharedDebugXCFramework")

                val xcframeworkPath = projectPath.resolve("build/XCFrameworks/debug/Shared.xcframework")
                val iosArm64FrameworkPath = xcframeworkPath.resolve("ios-arm64/Shared.framework")
                val iosSimulatorArm64FrameworkPath = xcframeworkPath.resolve("ios-arm64_x86_64-simulator/Shared.framework")

                // Assert that the XCFramework and the frameworks for the iOS targets are created
                assertDirectoryExists(iosArm64FrameworkPath, "ios-arm64/Shared.framework not found in $xcframeworkPath")
                assertDirectoryExists(
                    iosSimulatorArm64FrameworkPath,
                    "ios-arm64_x86_64-simulator/Shared.framework not found in $xcframeworkPath"
                )

                val iosArm64ResourcesPath = iosArm64FrameworkPath.resolve("embedResources")
                val iosSimulatorArm64ResourcesPath = iosSimulatorArm64FrameworkPath.resolve("embedResources")

                // Assert that the resources are published for the iOS targets
                assertDirectoryExists(iosArm64ResourcesPath, "embedResources not found in $iosArm64FrameworkPath")
                assertDirectoryExists(iosSimulatorArm64ResourcesPath, "embedResources not found in $iosSimulatorArm64FrameworkPath")

                // Assert that the resources are the same for both iOS targets
                assertEqualDirectories(
                    iosArm64ResourcesPath.toFile(),
                    iosSimulatorArm64ResourcesPath.toFile(),
                    forgiveExtraFiles = false
                )
            }
        }
    }

    /**
     * Tests the publication of multiplatform resources for a single iOS target embedded
     * within an XCFramework.
     */
    @DisplayName("Multiplatform resources publication for single target embedded in XCFramework")
    @GradleTest
    fun testXCFrameworkSingleTargetsResourcesPublication(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            configureForResources()

            build("assembleSharedDebugXCFramework") {
                assertTasksExecuted(":assembleSharedDebugXCFramework")

                val xcframeworkPath = projectPath.resolve("build/XCFrameworks/debug/Shared.xcframework")
                val iosArm64FrameworkPath = xcframeworkPath.resolve("ios-arm64/Shared.framework")
                val iosSimulatorArm64FrameworkPath = xcframeworkPath.resolve("ios-arm64_x86_64-simulator/Shared.framework")

                // Assert that the XCFramework and the frameworks for the iOS targets are created
                assertDirectoryExists(iosArm64FrameworkPath, "ios-arm64/Shared.framework not found in $xcframeworkPath")
                assertDirectoryDoesNotExist(iosSimulatorArm64FrameworkPath)

                val iosArm64ResourcesPath = iosArm64FrameworkPath.resolve("embedResources")
                val iosSimulatorArm64ResourcesPath = iosSimulatorArm64FrameworkPath.resolve("embedResources")

                // Assert that the resources are published for the iOS targets
                assertDirectoryExists(iosArm64ResourcesPath, "embedResources not found in $iosArm64FrameworkPath")
                assertDirectoryDoesNotExist(iosSimulatorArm64ResourcesPath)
            }
        }
    }

    /**
     * Tests the mutation of multiplatform resources embedded within an XCFramework, ensuring proper publication and resource handling.
     */
    @DisplayName("Multiplatform resources mutation in XCFramework")
    @GradleTest
    fun testXCFrameworkMutationResourcesPublication(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            configureForResources()

            val xcframeworkPath = projectPath.resolve("build/XCFrameworks/debug/Shared.xcframework")
            val iosArm64ResourcesPath = xcframeworkPath.resolve("ios-arm64/Shared.framework/embedResources")

            val drawables = iosArm64ResourcesPath.resolve("drawable")
            val files = iosArm64ResourcesPath.resolve("files")
            val font = iosArm64ResourcesPath.resolve("font")
            val values = iosArm64ResourcesPath.resolve("values")

            build("assembleSharedDebugXCFramework") {
                assertTasksExecuted(":assembleSharedDebugXCFramework")

                assertDirectoriesExist(
                    drawables, files, font, values,
                )

                assertFileExists(drawables.resolve("compose-multiplatform.xml"))
                assertFileExists(files.resolve("commonResource"))
                assertFileExists(font.resolve("IndieFlower-Regular.ttf"))
                assertFileExists(values.resolve("strings.xml"))
            }

            projectPath.resolve("appResources/drawable").deleteRecursively()
            projectPath.resolve("appResources/font").deleteRecursively()

            projectPath.resolve("appResources/files").resolve("commonResource").deleteExisting()
            projectPath.resolve("appResources/files").resolve("newCommonResource").createFile()
                .writeText("new commonResource")

            build("assembleSharedDebugXCFramework") {
                assertTasksExecuted(":assembleSharedDebugXCFramework")

                assertDirectoriesExist(
                    files, values,
                )

                assertDirectoryDoesNotExist(drawables)
                assertDirectoryDoesNotExist(font)

                assertFileNotExists(files.resolve("commonResource"))
                assertFileExists(files.resolve("newCommonResource"))
                assertFileExists(values.resolve("strings.xml"))
            }
        }
    }

    /**
     * Executes XCTests to validate the functionality and handling of xcframework with resources.
     */
    @DisplayName("run XCTests for testing xcframework with resources")
    @GradleTest
    fun testXcframeworkResourcesXCTests(
        gradleVersion: GradleVersion,
    ) {
        XCTestHelpers().use {
            val simulator = it.createSimulator().apply {
                boot()
            }

            project("empty", gradleVersion) {
                embedDirectoryFromTestData("resourcesXCFramework/iosApp", "iosApp")
                configureForResources {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64(),
                    )
                }

                build("assembleSharedDebugXCFramework") {
                    assertTasksExecuted(":assembleSharedDebugXCFramework")
                }

                val xcframeworkPath = projectPath.resolve("build/XCFrameworks/debug/Shared.xcframework")
                assertDirectoryExists(xcframeworkPath, "xcframework not found in $projectPath")

                xcframeworkPath.copyToRecursively(projectPath.resolve("iosApp/Shared.xcframework"), followLinks = false, overwrite = true)

                // Direct xcframework integration
                buildXcodeProject(
                    xcodeproj = projectPath.resolve("iosApp/XCTestApp.xcodeproj"),
                    scheme = "XCTestAppTests",
                    destination = "platform=iOS Simulator,id=${simulator.udid}",
                    action = XcodeBuildAction.Test
                )

                // SPM integration with .binaryTarget
                buildXcodeProject(
                    xcodeproj = projectPath.resolve("iosApp/XCTestApp_spm.xcodeproj"),
                    scheme = "XCTestAppTests",
                    destination = "platform=iOS Simulator,id=${simulator.udid}",
                    action = XcodeBuildAction.Test
                )

                val frameworkPath = projectPath
                    .resolve("xcodeDerivedData/Build/Products/Debug-iphonesimulator/XCTestAppTests.xctest/Frameworks/Shared.framework")

                val stubSymbols = runProcess(
                    listOf("nm", "-U", "Shared"),
                    frameworkPath.toFile()
                )

                // Ensure the binary does not export any symbols.
                // We ignore blank lines and file headers like "Shared:",
                // but any actual symbol lines should fail the test.
                val cleanedOutput = stubSymbols.output
                    .lines()
                    .map { line -> line.trim() }
                    .filter { line -> line.isNotEmpty() && !line.endsWith(":") } // filter out empty and filename lines
                    .joinToString("\n")

                assertEquals(
                    "",
                    cleanedOutput,
                    "Shared is not a stub binary – unexpected symbols:\n$cleanedOutput"
                )
            }
        }
    }
}

private fun TestProject.configureForResources(
    multiplatform: KotlinMultiplatformExtension.() -> List<KotlinNativeTarget> = {
        listOf(
            iosArm64()
        )
    },
) {
    embedDirectoryFromTestData("resourcesXCFramework/appResources", "appResources")
    plugins {
        kotlin("multiplatform")
    }
    settingsBuildScriptInjection {
        settings.rootProject.name = "xcframeworkresources"
    }
    buildScriptInjection {
        project.applyMultiplatform {
            val xcf = project.XCFramework("Shared")

            val publication = project.extraProperties.get(
                KotlinTargetResourcesPublication.EXTENSION_NAME
            ) as KotlinTargetResourcesPublication

            multiplatform().forEach { target ->
                target.binaries.framework {
                    baseName = "Shared"
                    isStatic = true
                    xcf.add(this)
                }

                publication.publishResourcesAsKotlinComponent(
                    target = target,
                    resourcePathForSourceSet = { sourceSet ->
                        KotlinTargetResourcesPublication.ResourceRoot(
                            resourcesBaseDirectory = project.provider { project.file("appResources") },
                            includes = emptyList(),
                            excludes = emptyList(),
                        )
                    },
                    relativeResourcePlacement = project.provider { File("embedResources") },
                )

                val xcTask = project.tasks.getByName("assembleSharedDebugXCFramework") as XCFrameworkTask
                xcTask.addTargetResources(publication.resolveResources(target), target.konanTarget)
            }

            sourceSets.commonMain.get().compileSource("class Greeting()")
        }
    }
}
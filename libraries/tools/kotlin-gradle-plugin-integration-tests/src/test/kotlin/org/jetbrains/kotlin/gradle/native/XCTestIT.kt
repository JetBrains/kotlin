/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceFirst
import org.jetbrains.kotlin.konan.target.Xcode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.deleteRecursively

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Integration of the Kotlin/Native XCTest support in tests")
@NativeGradlePluginTests
internal class XCTestIT : KGPBaseTest() {

    private val devFrameworkPath by lazy { "${Xcode.findCurrent().iphonesimulatorSdkPlatform}/Developer/Library/Frameworks/" }

    @DisplayName("A simple test bundle producer")
    @GradleTest
    fun shouldProduceTestBundle(gradleVersion: GradleVersion) {
        nativeProject("native-apple-test-bundle", gradleVersion) {
            // Put the path to Developer Frameworks where XCTest.framework is located
            buildGradleKts.replaceFirst("<PATH_TO_FRAMEWORKS>", devFrameworkPath)

            // Build a project and check that a Test bundle was created
            build("linkSimpleTestDebugTestBundleIosSimulatorArm64", forceOutput = true) {
                assertDirectoryInProjectExists("build/bin/iosSimulatorArm64/simpleTestDebugTestBundle/simpleTest.xctest")
            }
        }
    }

    @DisplayName("Test bundle running under xcodebuild without the host application")
    @GradleTest
    fun testBundleWithXcodebuild(gradleVersion: GradleVersion) {
        XCTestHelpers().use {
            nativeProject("native-apple-test-bundle-xcode", gradleVersion) {
                // Put the path to Developer Frameworks where XCTest.framework is located
                buildGradleKts.replaceFirst("<PATH_TO_FRAMEWORKS>", devFrameworkPath)

                // Build a project and check that a Test bundle was created
                val xctestBundleProjectPath = "build/bin/iosSimulatorArm64/iosAppTestsDebugTestBundle/iosAppTests.xctest"
                build("linkIosAppTestsDebugTestBundleIosSimulatorArm64", forceOutput = true) {
                    assertDirectoryInProjectExists("build/bin/iosSimulatorArm64/iosAppTestsDebugTestBundle/iosAppTests.xctest")
                }

                // Prepare the simulator (boot it) to be able to run UI tests
                val simulator = it.createSimulator().apply {
                    boot()
                }

                val derivedDataPath = projectPath.resolve("iosApp/DerivedData")
                // Build the project so that Xcode creates the whole build directory structure with application and tests
                xcodebuild(
                    xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
                    scheme = "iosAppTests",
                    destination = "generic/platform=iOS Simulator",
                    action = XcodeBuildAction.BuildForTesting,
                    derivedDataPath = derivedDataPath
                )

                // Copy test bundle into the Xcode build output directory
                val destination = derivedDataPath.resolve("Build/Products/Debug-iphonesimulator/iosAppTests.xctest")
                destination.deleteRecursively()
                projectPath.resolve(xctestBundleProjectPath).copyRecursively(destination)

                // Now, run tests
                xcodebuild(
                    xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
                    scheme = "iosAppTests",
                    destination = "platform=iOS Simulator,id=${simulator.udid}",
                    action = XcodeBuildAction.TestWithoutBuilding,
                    derivedDataPath = derivedDataPath
                )
            }
        }
    }

    @DisplayName("Test bundle running under xcodebuild with the host application")
    @GradleTest
    fun testBundleWithXcodebuildWithHostApp(gradleVersion: GradleVersion) {
        XCTestHelpers().use {
            nativeProject("native-apple-test-bundle-xcode-hostapp", gradleVersion) {
                // Put the path to Developer Frameworks where XCTest.framework is located
                buildGradleKts.replaceFirst("<PATH_TO_FRAMEWORKS>", devFrameworkPath)

                // Build a project and check that a Test bundle was created
                val xctestBundleProjectPath = "build/bin/iosSimulatorArm64/iosAppTestsDebugTestBundle/iosAppTests.xctest"
                build("linkIosAppTestsDebugTestBundleIosSimulatorArm64", forceOutput = true) {
                    assertDirectoryInProjectExists("build/bin/iosSimulatorArm64/iosAppTestsDebugTestBundle/iosAppTests.xctest")
                }

                // Prepare the simulator (boot it) to be able to run UI tests
                val simulator = it.createSimulator().apply {
                    boot()
                }

                val derivedDataPath = projectPath.resolve("iosApp/DerivedData")
                // Build the project so that Xcode creates the whole build directory structure with application and tests
                xcodebuild(
                    xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
                    scheme = "iosAppTests",
                    destination = "generic/platform=iOS Simulator",
                    action = XcodeBuildAction.BuildForTesting,
                    derivedDataPath = derivedDataPath
                )

                // Copy test bundle into the Xcode build output directory into Application's PlugIns directory
                val destination = derivedDataPath.resolve("Build/Products/Debug-iphonesimulator/iosApp.app/PlugIns/iosAppTests.xctest")
                destination.deleteRecursively()
                projectPath.resolve(xctestBundleProjectPath).copyRecursively(destination)

                // Now, run tests
                xcodebuild(
                    xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
                    scheme = "iosAppTests",
                    destination = "platform=iOS Simulator,id=${simulator.udid}",
                    action = XcodeBuildAction.TestWithoutBuilding,
                    derivedDataPath = derivedDataPath
                )
            }
        }
    }
}
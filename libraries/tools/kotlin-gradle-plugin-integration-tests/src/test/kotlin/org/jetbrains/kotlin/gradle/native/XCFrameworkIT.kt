/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceFirst
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for K/N with apple XCFramework")
@NativeGradlePluginTests
class XCFrameworkIT : KGPBaseTest() {

    @DisplayName("Assembling shared debug XCFramework for all available ios and watchos targets ")
    @GradleTest
    fun shouldAssembleXCFrameworkForAllAvailableTargets(gradleVersion: GradleVersion) {
        project("appleXCFramework", gradleVersion) {
            build("assembleSharedDebugXCFramework") {
                assertTasksExecuted(":shared:linkDebugFrameworkIosX64")
                assertTasksExecuted(":shared:linkDebugFrameworkIosArm64")
                assertTasksExecuted(":shared:linkDebugFrameworkIosSimulatorArm64")
                assertTasksExecuted(":shared:assembleDebugIosSimulatorFatFrameworkForSharedXCFramework")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosArm32")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosArm64")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosDeviceArm64")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosSimulatorArm64")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosX64")
                assertTasksExecuted(":shared:assembleDebugWatchosFatFrameworkForSharedXCFramework")
                assertTasksExecuted(":shared:assembleDebugWatchosSimulatorFatFrameworkForSharedXCFramework")
                assertTasksExecuted(":shared:assembleSharedDebugXCFramework")
                assertDirectoryInProjectExists("shared/build/XCFrameworks/debug/shared.xcframework")
                assertDirectoryInProjectExists("shared/build/XCFrameworks/debug/shared.xcframework/ios-arm64_x86_64-simulator/dSYMs/shared.framework.dSYM")
                assertDirectoryInProjectExists("shared/build/sharedXCFrameworkTemp/fatframework/debug/watchos/shared.framework")
                assertDirectoryInProjectExists("shared/build/sharedXCFrameworkTemp/fatframework/debug/watchos/shared.framework.dSYM")
            }

            build("assembleSharedDebugXCFramework") {
                assertTasksUpToDate(":shared:linkDebugFrameworkIosArm64")
                assertTasksUpToDate(":shared:linkDebugFrameworkIosX64")
                assertTasksUpToDate(":shared:linkDebugFrameworkWatchosArm32")
                assertTasksUpToDate(":shared:linkDebugFrameworkWatchosArm64")
                assertTasksUpToDate(":shared:linkDebugFrameworkWatchosDeviceArm64")
                assertTasksUpToDate(":shared:linkDebugFrameworkWatchosX64")
                assertTasksUpToDate(":shared:assembleDebugWatchosFatFrameworkForSharedXCFramework")
                assertTasksUpToDate(":shared:assembleSharedDebugXCFramework")
            }
        }
    }

    @DisplayName("Assembling other XCFramework for ios targets")
    @GradleTest
    fun shouldAssembleOtherXCFrameworkForIosTargets(gradleVersion: GradleVersion) {
        project("appleXCFramework", gradleVersion) {
            build("assembleOtherXCFramework") {
                assertTasksExecuted(":shared:linkReleaseFrameworkIosArm64")
                assertTasksExecuted(":shared:linkReleaseFrameworkIosX64")
                assertTasksExecuted(":shared:assembleOtherReleaseXCFramework")
                assertDirectoryInProjectExists("shared/build/XCFrameworks/release/other.xcframework")
                assertDirectoryInProjectExists("shared/build/XCFrameworks/release/other.xcframework/ios-arm64/dSYMs/shared.framework.dSYM")
                assertTasksExecuted(":shared:linkDebugFrameworkIosArm64")
                assertTasksExecuted(":shared:linkDebugFrameworkIosX64")
                assertTasksExecuted(":shared:assembleOtherDebugXCFramework")
                assertDirectoryInProjectExists("shared/build/XCFrameworks/debug/other.xcframework")
                assertDirectoryInProjectExists("shared/build/XCFrameworks/debug/other.xcframework/ios-arm64/dSYMs/shared.framework.dSYM")
                assertOutputContains("Name of XCFramework 'other' differs from inner frameworks name 'shared'! Framework renaming is not supported yet")
            }
        }
    }

    @DisplayName("XCFramework doesn't contain any framework's tasks without declaration in build script")
    @GradleAndroidTest
    fun shouldNotContainNotDeclaredTasks(gradleVersion: GradleVersion) {
        project("appleXCFramework", gradleVersion) {
            build("tasks") {
                assertTasksInBuildOutput(
                    expectedAbsentTasks = listOf(
                        "shared:assembleSharedDebugXCFramework",
                        "shared:assembleSharedReleaseXCFramework",
                        "shared:assembleXCFramework"
                    )
                )
            }

        }
    }

    @DisplayName("Two XCFrameworks were registered with same name")
    @GradleTest
    fun shouldCheckConfigurationErrorWithTwoRegisteredXCFrameworks(gradleVersion: GradleVersion) {
        project("appleXCFramework", gradleVersion) {
            subProject("shared")
                .buildGradleKts
                .replaceText("XCFramework(\"other\")", "XCFramework()")

            buildAndFail("tasks") {
                assertOutputContains("Cannot add task 'assembleSharedReleaseXCFramework' as a task with that name already exists.")
            }
        }
    }

    @DisplayName("K/N project with XCFramework, that contains frameworks with different names")
    @GradleTest
    fun shouldCheckConfigurationErrorForXCFrameworkWithDifferentFrameworksNames(gradleVersion: GradleVersion) {
        project("appleXCFramework", gradleVersion) {
            subProject("shared")
                .buildGradleKts
                .replaceFirst("baseName = \"shared\"", "baseName = \"awesome\"")

            buildAndFail("tasks") {
                assertOutputContains("All inner frameworks in XCFramework 'shared' should have same names. But there are two with 'awesome' and 'shared' names")
            }
        }
    }

    @DisplayName("Assembling framework does not produce an error or fail when there are no sources to assemble")
    @GradleTest
    fun shouldNotProduceErrorsWithoutSources(gradleVersion: GradleVersion) {
        project("appleXCFramework", gradleVersion) {
            projectPath.resolve("shared/src").deleteRecursively()
            build(":shared:assembleXCFramework") {
                assertTasksNoSource(
                    ":shared:assembleSharedDebugXCFramework",
                    ":shared:assembleSharedReleaseXCFramework",
                )
            }
        }
    }

    @DisplayName("XCFramework handles dashes in its name correctly")
    @GradleTest
    fun shouldCheckDashesHandlingWithXCFramework(gradleVersion: GradleVersion) {
        project("appleXCFramework", gradleVersion) {

            val sharedBuildGradleKts = subProject("shared").buildGradleKts
            sharedBuildGradleKts.replaceText("baseName = \"shared\"", "baseName = \"sha-red\"")
            sharedBuildGradleKts.replaceText("XCFramework()", "XCFramework(\"sha-red\")")

            build(":shared:assembleSha-redXCFramework") {
                assertTasksExecuted(
                    ":shared:assembleSha-redDebugXCFramework",
                    ":shared:assembleSha-redReleaseXCFramework",
                )
                assertOutputDoesNotContain("differs from inner frameworks name")
            }
        }
    }
}
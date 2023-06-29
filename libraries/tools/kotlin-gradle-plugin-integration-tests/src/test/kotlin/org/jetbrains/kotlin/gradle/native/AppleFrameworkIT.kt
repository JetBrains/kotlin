/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for K/N with Apple Framework")
@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
@NativeGradlePluginTests
class AppleFrameworkIT : KGPBaseTest() {

    @DisplayName("Assembling AppleFrameworkForXcode tasks for IosArm64")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun shouldAssembleAppleFrameworkForXcodeForIosArm64(
        gradleVersion: GradleVersion,
    ) {

        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
            buildOptions = defaultBuildOptions,
            environmentVariables = EnvironmentalVariables(
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos123",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use"
            ),
        ) {

            build("assembleDebugAppleFrameworkForXcodeIosArm64") {
                assertTasksExecuted(":shared:assembleDebugAppleFrameworkForXcodeIosArm64")
                assertDirectoryInProjectExists("shared/build/xcode-frameworks/debug/iphoneos123/sdk.framework")
                assertDirectoryInProjectExists("shared/build/xcode-frameworks/debug/iphoneos123/sdk.framework.dSYM")
            }

            build("assembleCustomDebugAppleFrameworkForXcodeIosArm64") {
                assertTasksExecuted(":shared:assembleCustomDebugAppleFrameworkForXcodeIosArm64")
                assertDirectoryInProjectExists("shared/build/xcode-frameworks/debug/iphoneos123/lib.framework")
                assertDirectoryInProjectExists("shared/build/xcode-frameworks/debug/iphoneos123/lib.framework.dSYM")
            }
        }
    }

    @DisplayName("Assembling fat AppleFrameworkForXcode tasks for Arm64 and X64 simulators")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun shouldAssembleAppleFrameworkForXcodeForArm64AndX64Simulators(
        gradleVersion: GradleVersion,
    ) {

        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            val environmentVariables = mapOf(
                "CONFIGURATION" to "Release",
                "SDK_NAME" to "iphonesimulator",
                "ARCHS" to "arm64 x86_64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use"
            )
            build("assembleReleaseAppleFrameworkForXcode", environmentVariables = EnvironmentalVariables(environmentVariables)) {
                assertTasksExecuted(":shared:linkReleaseFrameworkIosSimulatorArm64")
                assertTasksExecuted(":shared:linkReleaseFrameworkIosX64")
                assertTasksExecuted(":shared:assembleReleaseAppleFrameworkForXcode")
                assertDirectoryInProjectExists("shared/build/xcode-frameworks/Release/iphonesimulator/sdk.framework")
                assertDirectoryInProjectExists("shared/build/xcode-frameworks/Release/iphonesimulator/sdk.framework.dSYM")
            }
        }
    }

    @DisplayName("MacOS framework has symlinks")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun shouldCheckThatMacOSFrameworkHasSymlinks(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {

        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
        ) {
            val environmentVariables = mapOf(
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "macosx",
                "ARCHS" to "x86_64",
                "EXPANDED_CODE_SIGN_IDENTITY" to "-",
                "TARGET_BUILD_DIR" to testBuildDir.toString(),
                "FRAMEWORKS_FOLDER_PATH" to "build/xcode-derived"
            )
            build(":shared:embedAndSignAppleFrameworkForXcode", environmentVariables = EnvironmentalVariables(environmentVariables)) {
                assertTasksExecuted(":shared:assembleDebugAppleFrameworkForXcodeMacosX64")
                assertSymlinkInProjectExists("shared/build/xcode-frameworks/debug/macosx/sdk.framework/Headers")
                assertSymlinkExists(testBuildDir.resolve("build/xcode-derived/sdk.framework/Headers"))
            }
        }
    }

    @DisplayName("embedAndSign executes normally when signing is disabled")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun testEmbedAnsSignExecutionWithoutSigning(
        gradleVersion: GradleVersion,
    ) {

        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
        ) {
            val environmentVariables = mapOf(
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to projectPath.absolutePathString(),
                "FRAMEWORKS_FOLDER_PATH" to "build/xcode-derived"
            )
            build(":shared:embedAndSignAppleFrameworkForXcode", environmentVariables = EnvironmentalVariables(environmentVariables)) {
                assertDirectoryInProjectExists("build/xcode-derived/sdk.framework")
            }
        }
    }

    @DisplayName("embedAndSignAppleFrameworkForXcode fail")
    @GradleTest
    fun shouldFailWithExecutingEmbedAndSignAppleFrameworkForXcode(
        gradleVersion: GradleVersion,
    ) {
        nativeProject("sharedAppleFramework", gradleVersion) {
            buildAndFail(":shared:embedAndSignAppleFrameworkForXcode") {
                assertOutputContains("Please run the embedAndSignAppleFrameworkForXcode task from Xcode")
            }
        }
    }

    @DisplayName("Registered tasks with Xcode environment for Debug IosArm64 configuration")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun shouldCheckAllRegisteredTasksWithXcodeEnvironmentForDebugIosArm64(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
        ) {
            val environmentVariables = mapOf(
                "CONFIGURATION" to "Debug",
                "SDK_NAME" to "iphoneos",
                "ARCHS" to "arm64",
                "EXPANDED_CODE_SIGN_IDENTITY" to "-",
                "TARGET_BUILD_DIR" to testBuildDir.toString(),
                "FRAMEWORKS_FOLDER_PATH" to "testFrameworksDir"
            )
            buildAndAssertAllTasks(
                registeredTasks = listOf(
                    "shared:assembleDebugAppleFrameworkForXcodeIosArm64",
                    "shared:embedAndSignAppleFrameworkForXcode",
                    "shared:assembleCustomDebugAppleFrameworkForXcodeIosArm64",
                    "shared:embedAndSignCustomAppleFrameworkForXcode",
                    "shared:assembleDebugAppleFrameworkForXcodeIosX64",
                    "shared:assembleReleaseAppleFrameworkForXcodeIosX64",
                    "shared:assembleReleaseAppleFrameworkForXcodeIosArm64",
                    "shared:assembleCustomDebugAppleFrameworkForXcodeIosX64",
                    "shared:assembleCustomReleaseAppleFrameworkForXcodeIosX64",
                    "shared:assembleCustomReleaseAppleFrameworkForXcodeIosArm64"
                ),
                environmentVariables = EnvironmentalVariables(environmentVariables)
            )
        }
    }

    @DisplayName("embedAndSignAppleFrameworkForXcode was registered without required Xcode environments")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun shouldCheckEmbedAndSignAppleFrameworkForXcodeDoesNotRequireXcodeEnv(
        gradleVersion: GradleVersion,
    ) {
        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
        ) {
            val environmentVariables = EnvironmentalVariables(
                mapOf(
                    "CONFIGURATION" to "Debug",
                    "SDK_NAME" to "iphoneos",
                    "ARCHS" to "arm64"
                )
            )
            buildAndAssertAllTasks(
                registeredTasks = listOf(
                    "shared:embedAndSignAppleFrameworkForXcode",
                    "shared:embedAndSignCustomAppleFrameworkForXcode"
                ),
                notRegisteredTasks = listOf(
                    "shared:assembleReleaseAppleFrameworkForXcodeIosX64",
                    "shared:assembleDebugAppleFrameworkForXcodeIosX64",
                    "shared:assembleReleaseAppleFrameworkForXcodeIosArm64",
                    "shared:assembleDebugAppleFrameworkForXcodeIosArm64",
                    "shared:assembleCustomDebugAppleFrameworkForXcodeIosX64",
                    "shared:assembleCustomReleaseAppleFrameworkForXcodeIosX64",
                    "shared:assembleCustomDebugAppleFrameworkForXcodeIosArm64",
                    "shared:assembleCustomReleaseAppleFrameworkForXcodeIosArm64"
                ),
                environmentVariables = environmentVariables
            )
            buildAndFail(
                ":shared:embedAndSignCustomAppleFrameworkForXcode",
                environmentVariables = environmentVariables
            ) {
                assertTasksFailed(":shared:embedAndSignCustomAppleFrameworkForXcode")
                assertOutputContains("Please run the embedAndSignCustomAppleFrameworkForXcode task from Xcode")
            }
        }
    }

    @DisplayName("Static framework for Arm64 is built but is not embedded")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun shouldCheckThatStaticFrameworkForArm64IsBuildAndNotEmbedded(
        gradleVersion: GradleVersion,
    ) {

        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            val environmentVariables = EnvironmentalVariables(
                mapOf(
                    "CONFIGURATION" to "debug",
                    "SDK_NAME" to "iphoneos123",
                    "ARCHS" to "arm64",
                    "TARGET_BUILD_DIR" to "no use",
                    "FRAMEWORKS_FOLDER_PATH" to "no use"
                )
            )

            subProject("shared").buildGradleKts.modify {
                it.replace(
                    "baseName = \"sdk\"",
                    "baseName = \"sdk\"\nisStatic = true"
                )
            }

            build("embedAndSignAppleFrameworkForXcode", environmentVariables = environmentVariables) {
                assertTasksExecuted(":shared:assembleDebugAppleFrameworkForXcodeIosArm64")
                assertTasksSkipped(":shared:embedAndSignAppleFrameworkForXcode")
                assertDirectoryInProjectExists("shared/build/xcode-frameworks/debug/iphoneos123/sdk.framework")
                assertFileInProjectNotExists("shared/build/xcode-frameworks/debug/iphoneos123/sdk.framework.dSYM")
            }
        }
    }

    @DisplayName("Configuration errors reported to Xcode when embedAndSign task requested")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun shouldReportConfErrorsToXcodeWhenRequestedByEmbedAndSign(
        gradleVersion: GradleVersion,
    ) {
        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
        ) {
            val environmentVariables = EnvironmentalVariables(
                mapOf(
                    "CONFIGURATION" to "debug",
                    "SDK_NAME" to "iphoneos123",
                    "ARCHS" to "arm64",
                    "TARGET_BUILD_DIR" to "no use",
                    "FRAMEWORKS_FOLDER_PATH" to "no use"
                )
            )

            subProject("shared").buildGradleKts.appendText(
                """
                kotlin {
                    sourceSets["commonMain"].dependencies {
                        implementation("com.example.unknown:dependency:0.0.1")
                    }       
                }
                """.trimIndent()
            )

            buildAndFail(":shared:embedAndSignAppleFrameworkForXcode", environmentVariables = environmentVariables) {
                assertOutputContains("error: Could not find com.example.unknown:dependency:0.0.1.")
            }
        }
    }

    @DisplayName("Compilation errors reported to Xcode when embedAndSign task requested")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun shouldReportCompilationErrorsToXcodeWhenRequestedByEmbedAndSign(
        gradleVersion: GradleVersion,
    ) {
        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
        ) {
            val environmentVariables = EnvironmentalVariables(
                mapOf(
                    "CONFIGURATION" to "debug",
                    "SDK_NAME" to "iphoneos123",
                    "ARCHS" to "arm64",
                    "TARGET_BUILD_DIR" to "no use",
                    "FRAMEWORKS_FOLDER_PATH" to "no use"
                )
            )

            projectPath.resolve("shared/src/commonMain/kotlin/com/github/jetbrains/myapplication/Greeting.kt")
                .appendText("this can't be compiled")

            buildAndFail(":shared:embedAndSignAppleFrameworkForXcode", environmentVariables = environmentVariables) {
                assertOutputContains("/sharedAppleFramework/shared/src/commonMain/kotlin/com/github/jetbrains/myapplication/Greeting.kt:7:2: error: Expecting a top level declaration")
                assertOutputContains("error: Compilation finished with errors")
            }
        }
    }

    @DisplayName("Compilation errors printed with Gradle-style when any other task requested")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun shouldPrintCompilationErrorsWithGradleStyle(
        gradleVersion: GradleVersion,
    ) {
        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
        ) {
            val environmentVariables = EnvironmentalVariables(
                mapOf(
                    "CONFIGURATION" to "debug",
                    "SDK_NAME" to "iphoneos123",
                    "ARCHS" to "arm64",
                    "TARGET_BUILD_DIR" to "no use",
                    "FRAMEWORKS_FOLDER_PATH" to "no use"
                )
            )

            projectPath.resolve("shared/src/commonMain/kotlin/com/github/jetbrains/myapplication/Greeting.kt")
                .appendText("this can't be compiled")

            buildAndFail(":shared:assembleDebugAppleFrameworkForXcodeIosArm64", environmentVariables = environmentVariables) {
                assertOutputContains("e: file:///")
                assertOutputDoesNotContain("error: Compilation finished with errors")
            }
        }
    }

    @DisplayName("Compilation errors printed with Xcode-style with explicit option")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun shouldPrintCompilationErrorsWithXcodeStyle(
        gradleVersion: GradleVersion,
    ) {
        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
        ) {
            val environmentVariables = EnvironmentalVariables(
                mapOf(
                    "CONFIGURATION" to "debug",
                    "SDK_NAME" to "iphoneos123",
                    "ARCHS" to "arm64",
                    "TARGET_BUILD_DIR" to "no use",
                    "FRAMEWORKS_FOLDER_PATH" to "no use"
                )
            )

            projectPath.resolve("shared/src/commonMain/kotlin/com/github/jetbrains/myapplication/Greeting.kt")
                .appendText("this can't be compiled")

            buildAndFail(
                ":shared:assembleDebugAppleFrameworkForXcodeIosArm64",
                "-Pkotlin.native.useXcodeMessageStyle=true",
                environmentVariables = environmentVariables
            ) {
                assertOutputContains("/sharedAppleFramework/shared/src/commonMain/kotlin/com/github/jetbrains/myapplication/Greeting.kt:7:2: error: Expecting a top level declaration")
                assertOutputContains("error: Compilation finished with errors")
            }
        }
    }

    @DisplayName("Compilation errors reported to Xcode when embedAndSign task requested and compiler runs in a separate process")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun shouldReportErrorsToXcodeWhenEmbedAndSignRequestedAndDisableCompilerDaemon(
        gradleVersion: GradleVersion,
    ) {
        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
        ) {
            val environmentVariables = EnvironmentalVariables(
                mapOf(
                    "CONFIGURATION" to "debug",
                    "SDK_NAME" to "iphoneos123",
                    "ARCHS" to "arm64",
                    "TARGET_BUILD_DIR" to "no use",
                    "FRAMEWORKS_FOLDER_PATH" to "no use"
                )
            )

            projectPath.resolve("shared/src/commonMain/kotlin/com/github/jetbrains/myapplication/Greeting.kt")
                .appendText("this can't be compiled")

            buildAndFail(
                ":shared:embedAndSignAppleFrameworkForXcode",
                "-Pkotlin.native.disableCompilerDaemon=true",
                environmentVariables = environmentVariables
            ) {
                assertOutputContains("/sharedAppleFramework/shared/src/commonMain/kotlin/com/github/jetbrains/myapplication/Greeting.kt:7:2: error: Expecting a top level declaration")
            }
        }
    }

    @DisplayName("Frameworks can be consumed from other gradle project")
    @GradleTest
    fun shouldCheckFrameworksCanBeConsumedFromOtherGradleProjects(gradleVersion: GradleVersion) {
        nativeProject("consumableAppleFrameworks", gradleVersion) {
            build(":consumer:help") {
                assertOutputContains("RESOLUTION_SUCCESS")
                assertOutputDoesNotContain("RESOLUTION_FAILURE")
            }
        }
    }

    @DisplayName("Smoke test with apple gradle plugin")
    @GradleWithJdkTest
    @JdkVersions(versions = [JavaVersion.VERSION_11])
    fun smokeTestWithAppleGradlePlugin(
        gradleVersion: GradleVersion,
        providedJdk: JdkVersions.ProvidedJdk,
    ) {

        nativeProject(
            "appleGradlePluginConsumesAppleFrameworks",
            gradleVersion,
            buildJdk = providedJdk.location
        ) {
            fun dependencyInsight(configuration: String) = arrayOf(
                ":iosApp:dependencyInsight", "--configuration", configuration, "--dependency", "iosLib"
            )

            fun variant(variantName: String) =
                if (gradleVersion >= GradleVersion.version(TestVersions.Gradle.G_7_5)) {
                    "Variant $variantName"
                } else {
                    "variant \"$variantName\""
                }

            fun BuildResult.assertContainsVariant(variantName: String) = assertOutputContains(variant(variantName))

            subProject("iosApp").buildGradleKts.replaceText("<applePluginTestVersion>", "\"${TestVersions.AppleGradlePlugin.V222_0_21}\"")

            build(*dependencyInsight("iosAppIosX64DebugImplementation")) {
                assertContainsVariant("mainDynamicDebugFrameworkIos")
            }

            build(*dependencyInsight("iosAppIosX64ReleaseImplementation")) {
                assertContainsVariant("mainDynamicReleaseFrameworkIos")
            }

            // NB: '0' is required at the end since dependency is added with custom attribute, and it creates new configuration
            build(*dependencyInsight("iosAppIosX64DebugImplementation0"), "-PmultipleFrameworks") {
                assertContainsVariant("mainStaticDebugFrameworkIos")
            }

            build(*dependencyInsight("iosAppIosX64ReleaseImplementation0"), "-PmultipleFrameworks") {
                assertOutputDoesNotContain(variant("mainStaticReleaseFrameworkIos"))
            }
        }
    }
}
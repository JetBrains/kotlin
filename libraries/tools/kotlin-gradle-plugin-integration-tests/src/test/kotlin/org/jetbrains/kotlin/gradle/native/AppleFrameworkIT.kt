/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.TestVersions.Gradle
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.*


@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for K/N with Apple Framework")
@GradleTestVersions(minVersion = Gradle.G_7_0)
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
        ) {
            val environmentVariables = EnvironmentalVariables(
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos123",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use",
                "BUILT_PRODUCTS_DIR" to projectPath.resolve("shared/build/builtProductsDir").toString(),
            )

            build("assembleDebugAppleFrameworkForXcodeIosArm64", environmentVariables = environmentVariables) {
                assertTasksExecuted(":shared:assembleDebugAppleFrameworkForXcodeIosArm64")
                assertDirectoryInProjectExists("shared/build/builtProductsDir/sdk.framework")
                assertDirectoryInProjectExists("shared/build/builtProductsDir/sdk.framework.dSYM")
                assertFileInProjectContains(
                    "shared/build/builtProductsDir/sdk.framework/Modules/module.modulemap",
                    "framework module \"sdk\"",
                )
                assertDirectoryInProjectDoesNotExist("shared/build/xcode-frameworks/sdk.framework")
            }

            build("clean")

            build("assembleDebugAppleFrameworkForXcodeIosArm64", "-Pkotlin.apple.copyFrameworkToBuiltProductsDir=false", environmentVariables = environmentVariables) {
                assertTasksExecuted(":shared:assembleDebugAppleFrameworkForXcodeIosArm64")

                assertDirectoryInProjectExists("shared/build/xcode-frameworks/debug/iphoneos123/sdk.framework")
                assertDirectoryInProjectExists("shared/build/xcode-frameworks/debug/iphoneos123/sdk.framework.dSYM")

                assertDirectoryInProjectDoesNotExist("shared/build/builtProductsDir/sdk.framework")

            }

            build("assembleCustomDebugAppleFrameworkForXcodeIosArm64", environmentVariables = environmentVariables) {
                assertTasksExecuted(":shared:assembleCustomDebugAppleFrameworkForXcodeIosArm64")
                assertDirectoryInProjectExists("shared/build/builtProductsDir/lib.framework")
                assertDirectoryInProjectExists("shared/build/builtProductsDir/lib.framework.dSYM")
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
            val environmentVariables = EnvironmentalVariables(
                "CONFIGURATION" to "Release",
                "SDK_NAME" to "iphonesimulator",
                "ARCHS" to "arm64 x86_64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use",
                "BUILT_PRODUCTS_DIR" to projectPath.resolve("shared/build/xcode-frameworks/Release/iphonesimulator").toString(),
            )
            build("assembleReleaseAppleFrameworkForXcode", environmentVariables = environmentVariables) {
                assertTasksExecuted(":shared:linkReleaseFrameworkIosSimulatorArm64")
                assertTasksExecuted(":shared:linkReleaseFrameworkIosX64")
                assertTasksExecuted(":shared:assembleReleaseAppleFrameworkForXcode")
                assertDirectoryInProjectExists("shared/build/xcode-frameworks/Release/iphonesimulator/sdk.framework")
                assertDirectoryInProjectExists("shared/build/xcode-frameworks/Release/iphonesimulator/sdk.framework.dSYM")
                assertFileInProjectContains(
                    "shared/build/xcode-frameworks/Release/iphonesimulator/sdk.framework/Modules/module.modulemap",
                    "framework module \"sdk\"",
                )
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
            val environmentVariables = EnvironmentalVariables(
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "macosx",
                "ARCHS" to "x86_64",
                "EXPANDED_CODE_SIGN_IDENTITY" to "-",
                "TARGET_BUILD_DIR" to testBuildDir.toString(),
                "FRAMEWORKS_FOLDER_PATH" to "build/xcode-derived",
                "BUILT_PRODUCTS_DIR" to projectPath.resolve("shared/build/xcode-frameworks/debug/macosx").toString(),
            )
            build(":shared:embedAndSignAppleFrameworkForXcode", environmentVariables = environmentVariables) {
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
            val environmentVariables = EnvironmentalVariables(
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to projectPath.absolutePathString(),
                "FRAMEWORKS_FOLDER_PATH" to "build/xcode-derived",
                "BUILT_PRODUCTS_DIR" to iosBuildProductsDir().absolutePathString(),
            )
            build(":shared:embedAndSignAppleFrameworkForXcode", environmentVariables = environmentVariables) {
                assertDirectoryInProjectExists("build/xcode-derived/sdk.framework")
            }
        }
    }

    @DisplayName("embedAndSign task does not copy dSYM to Xcode frameworks folder")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun testEmbedAnsSignDoesNotCopyDsym(
        gradleVersion: GradleVersion,
    ) {

        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
        ) {
            val environmentVariables = EnvironmentalVariables(
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos",
                "ARCHS" to "arm64",
                "EXPANDED_CODE_SIGN_IDENTITY" to "-",
                "TARGET_BUILD_DIR" to projectPath.absolutePathString(),
                "FRAMEWORKS_FOLDER_PATH" to "build/xcode-derived",
                "BUILT_PRODUCTS_DIR" to iosBuildProductsDir().absolutePathString(),
            )
            build(":shared:embedAndSignAppleFrameworkForXcode", environmentVariables = environmentVariables) {
                assertDirectoryInProjectExists("build/xcode-derived/sdk.framework")
                assertDirectoryInProjectDoesNotExist("build/xcode-derived/sdk.framework.DSYM")
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
            val environmentVariables = EnvironmentalVariables(
                "CONFIGURATION" to "Debug",
                "SDK_NAME" to "iphoneos",
                "ARCHS" to "arm64",
                "EXPANDED_CODE_SIGN_IDENTITY" to "-",
                "TARGET_BUILD_DIR" to testBuildDir.toString(),
                "FRAMEWORKS_FOLDER_PATH" to "testFrameworksDir",
                "BUILT_PRODUCTS_DIR" to iosBuildProductsDir().absolutePathString(),
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
                environmentVariables = environmentVariables
            )
        }
    }

    @DisplayName("embedAndSignAppleFrameworkForXcode was registered with ENABLE_USER_SCRIPT_SANDBOXING=YES")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun shouldFailEmbedAndSignAppleFrameworkForXcodeWithUserScriptSandboxingEnabled(
        gradleVersion: GradleVersion,
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
                "TARGET_BUILD_DIR" to projectPath.absolutePathString(),
                "FRAMEWORKS_FOLDER_PATH" to "build/xcode-derived",
                "ENABLE_USER_SCRIPT_SANDBOXING" to "YES",
                "BUILT_PRODUCTS_DIR" to iosBuildProductsDir().absolutePathString(),
            )
            buildAndFail(
                ":shared:embedAndSignAppleFrameworkForXcode",
                environmentVariables = EnvironmentalVariables(environmentVariables)
            ) {
                assertTasksFailed(":shared:checkSandboxAndWriteProtection")
                assertOutputContains("You have sandboxing for user scripts enabled.")
            }
        }
    }

    @DisplayName("embedAndSignAppleFrameworkForXcode was registered with missing BUILT_PRODUCTS_DIR directory")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun shouldFailEmbedAndSignAppleFrameworkForXcodeWithMissingBuildProductsDir(
        gradleVersion: GradleVersion,
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
                "TARGET_BUILD_DIR" to projectPath.absolutePathString(),
                "FRAMEWORKS_FOLDER_PATH" to "build/xcode-derived",
                "BUILT_PRODUCTS_DIR" to iosBuildProductsDir(true).absolutePathString(),
            )
            buildAndFail(
                ":shared:embedAndSignAppleFrameworkForXcode",
                environmentVariables = EnvironmentalVariables(environmentVariables)
            ) {
                assertTasksFailed(":shared:checkSandboxAndWriteProtection")
                assertOutputContains("BUILT_PRODUCTS_DIR is not accessible, probably you have sandboxing for user scripts enabled.")
            }
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
                "CONFIGURATION" to "Debug",
                "SDK_NAME" to "iphoneos",
                "ARCHS" to "arm64",
                "BUILT_PRODUCTS_DIR" to iosBuildProductsDir().absolutePathString(),
            )

            buildAndAssertAllTasks(
                registeredTasks = listOf(
                    "shared:embedAndSignAppleFrameworkForXcode",
                    "shared:embedAndSignCustomAppleFrameworkForXcode",
                ),
                notRegisteredTasks = listOf(
                    "shared:assembleReleaseAppleFrameworkForXcodeIosX64",
                    "shared:assembleDebugAppleFrameworkForXcodeIosX64",
                    "shared:assembleReleaseAppleFrameworkForXcodeIosArm64",
                    "shared:assembleDebugAppleFrameworkForXcodeIosArm64",
                    "shared:assembleCustomDebugAppleFrameworkForXcodeIosX64",
                    "shared:assembleCustomReleaseAppleFrameworkForXcodeIosX64",
                    "shared:assembleCustomDebugAppleFrameworkForXcodeIosArm64",
                    "shared:assembleCustomReleaseAppleFrameworkForXcodeIosArm64",
                ),
                environmentVariables = environmentVariables,
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
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos123",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use",
                "BUILT_PRODUCTS_DIR" to projectPath.resolve("shared/build/xcode-frameworks/debug/iphoneos123").toString(),
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
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos123",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use",
                "BUILT_PRODUCTS_DIR" to iosBuildProductsDir().absolutePathString(),
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
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos123",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use",
                "BUILT_PRODUCTS_DIR" to iosBuildProductsDir().absolutePathString(),
            )

            projectPath.resolve("shared/src/commonMain/kotlin/com/github/jetbrains/myapplication/Greeting.kt")
                .appendText("this can't be compiled")

            buildAndFail(":shared:embedAndSignAppleFrameworkForXcode", environmentVariables = environmentVariables) {
                assertOutputContains("/sharedAppleFramework/shared/src/commonMain/kotlin/com/github/jetbrains/myapplication/Greeting.kt:7:2: error: Syntax error: Expecting a top level declaration")
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
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos123",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use",
                "BUILT_PRODUCTS_DIR" to iosBuildProductsDir().absolutePathString(),
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
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos123",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use",
                "BUILT_PRODUCTS_DIR" to iosBuildProductsDir().absolutePathString(),
            )

            projectPath.resolve("shared/src/commonMain/kotlin/com/github/jetbrains/myapplication/Greeting.kt")
                .appendText("this can't be compiled")

            buildAndFail(
                ":shared:assembleDebugAppleFrameworkForXcodeIosArm64",
                "-Pkotlin.native.useXcodeMessageStyle=true",
                environmentVariables = environmentVariables
            ) {
                assertOutputContains("/sharedAppleFramework/shared/src/commonMain/kotlin/com/github/jetbrains/myapplication/Greeting.kt:7:2: error: Syntax error: Expecting a top level declaration")
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
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos123",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use",
                "BUILT_PRODUCTS_DIR" to iosBuildProductsDir().absolutePathString(),
            )

            projectPath.resolve("shared/src/commonMain/kotlin/com/github/jetbrains/myapplication/Greeting.kt")
                .appendText("this can't be compiled")

            buildAndFail(
                ":shared:embedAndSignAppleFrameworkForXcode",
                "-Pkotlin.native.disableCompilerDaemon=true",
                environmentVariables = environmentVariables
            ) {
                assertOutputContains("/sharedAppleFramework/shared/src/commonMain/kotlin/com/github/jetbrains/myapplication/Greeting.kt:7:2: error: Syntax error: Expecting a top level declaration")
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

            subProject("iosApp").buildGradleKts.replaceText("<applePluginTestVersion>", "\"${TestVersions.AppleGradlePlugin.V222_0_21}\"")

            build(*dependencyInsight("iosAppIosX64DebugImplementation")) {
                assertOutputContainsNativeFrameworkVariant("mainDynamicDebugFrameworkIos", gradleVersion)
            }

            build(*dependencyInsight("iosAppIosX64ReleaseImplementation")) {
                assertOutputContainsNativeFrameworkVariant("mainDynamicReleaseFrameworkIos", gradleVersion)
            }

            // NB: '0' is required at the end since dependency is added with custom attribute, and it creates new configuration
            build(*dependencyInsight("iosAppIosX64DebugImplementation0"), "-PmultipleFrameworks") {
                assertOutputContainsNativeFrameworkVariant("mainStaticDebugFrameworkIos", gradleVersion)
            }

            build(*dependencyInsight("iosAppIosX64ReleaseImplementation0"), "-PmultipleFrameworks") {
                assertOutputDoesNotContain("mainStaticReleaseFrameworkIos")
            }
        }
    }

    // Should always be green because the CI Xcode version must be supported
    @DisplayName("Xcode version too high diagnostic isn't emitted")
    @GradleTest
    @GradleTestVersions(minVersion = Gradle.G_7_4)
    fun testXcodeVersionTooHighDiagnosticNotEmitted(gradleVersion: GradleVersion) {
        nativeProject(
            "sharedAppleFramework",
            gradleVersion,
            // enable CC to make sure that external process isn't run during configuration
            buildOptions = defaultBuildOptions.copy(configurationCache = true),
        ) {
            build(":shared:linkReleaseFrameworkIosSimulatorArm64") {
                assertNoDiagnostic(KotlinToolingDiagnostics.XcodeVersionTooHighWarning)
            }
        }
    }
}

private val GradleProject.darwinBuildProductsDir: Path
    get() = projectPath.resolve("DerivedSources").apply {
        if (notExists()) {
            createDirectory()
        }
    }

private fun GradleProject.iosBuildProductsDir(writeProtected: Boolean = false): Path = darwinBuildProductsDir.apply {
    if (writeProtected) {
        setPosixFilePermissions(setOf(PosixFilePermission.OWNER_READ))
    }
}
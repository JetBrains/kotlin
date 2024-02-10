/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText

// We temporarily disable it for windows until a proper fix is found for this issue:
// https://youtrack.jetbrains.com/issue/KT-60138/NativeDownloadAndPlatformLibsIT-fails-on-Windows-OS
@OsCondition(
    supportedOn = [OS.MAC, OS.LINUX], enabledOnCI = [OS.MAC, OS.LINUX]
)
@DisplayName("Tests for K/N builds with native downloading and platform libs")
@NativeGradlePluginTests
@Deprecated(
    message =
    """
    This is deprecated test class with regression checks for old downloading logic.
    We support it during migration to kotlin native toolchain.
    If you want to add test here, be sure that you have added similar test with `-Pkotlin.native.toolchain.enabled=true`.
    """,
    ReplaceWith("NativeDownloadAndPlatformLibsIT")
)
class NativeDownloadAndPlatformLibsIT : KGPBaseTest() {

    private val platformName: String = HostManager.platformName()
    private val currentCompilerVersion = NativeCompilerDownloader.DEFAULT_KONAN_VERSION

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.withBundledKotlinNative().copy(
            // Disabling toolchain feature for checking stable logic with downloading kotlin native
            freeArgs = listOf("-Pkotlin.native.toolchain.enabled=false"),
            // For each test in this class, we need to provide an isolated .konan directory,
            // so we create it within each test project folder
            konanDataDir = workingDir.resolve(".konan")
                .toFile()
                .apply { mkdirs() }.toPath(),
        )

    @OptIn(EnvironmentalVariablesOverride::class)
    @DisplayName("K/N Gradle project build (on Linux or Mac) with a dependency from a Maven")
    @GradleTest
    fun testSetupCommonOptionsForCaches(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        val anotherKonanDataDir = tempDir.resolve(".konan2")
        nativeProject(
            "native-with-maven-dependencies",
            gradleVersion = gradleVersion,
            environmentVariables = EnvironmentalVariables(Pair("KONAN_DATA_DIR", anotherKonanDataDir.absolutePathString()))
        ) {
            build(
                "linkDebugExecutableNative",
                buildOptions = defaultBuildOptions.copy(
                    nativeOptions = defaultBuildOptions.nativeOptions.copy(
                        cacheKind = null
                    )
                )
            ) {
                assertOutputDoesNotContain("w: Failed to build cache")
                assertTasksExecuted(":linkDebugExecutableNative")
                assertDirectoryDoesNotExist(anotherKonanDataDir)
            }
        }
    }

    @DisplayName("Downloading K/N with custom konanDataDir property")
    @GradleTest
    fun testLibrariesGenerationInCustomKonanDir(gradleVersion: GradleVersion) {
        platformLibrariesProject("linuxX64", gradleVersion = gradleVersion) {
            build("assemble", buildOptions = defaultBuildOptions.copy(konanDataDir = workingDir.resolve(".konan"))) {
                assertOutputContains("Kotlin/Native distribution: .*kotlin-native-prebuilt-$platformName".toRegex())
                assertOutputDoesNotContain("Generate platform libraries for ")

                // checking that konan was downloaded and native dependencies were not downloaded into ~/.konan dir
                assertDirectoryExists(workingDir.resolve(".konan/dependencies"))
                assertDirectoryExists(workingDir.resolve(".konan/kotlin-native-prebuilt-$platformName-$currentCompilerVersion"))
            }
        }
    }

    @DisplayName("K/N distribution without platform libraries generation")
    @GradleTest
    fun testNoGenerationByDefault(gradleVersion: GradleVersion) {
        platformLibrariesProject("linuxX64", gradleVersion = gradleVersion) {
            build("assemble") {
                assertOutputContains("Kotlin/Native distribution: .*kotlin-native-prebuilt-$platformName".toRegex())
                assertOutputDoesNotContain("Generate platform libraries for ")
            }
        }
    }

    @DisplayName("K/N distribution with platform libraries generation")
    @GradleTest
    fun testLibrariesGeneration(gradleVersion: GradleVersion) {
        nativeProject("native-platform-libraries", gradleVersion = gradleVersion) {

            includeOtherProjectAsSubmodule("native-platform-libraries", "", "subproject", true)

            buildGradleKts.appendText("\nkotlin.linuxX64()\n")
            subProject("subproject").buildGradleKts.appendText("\nkotlin.linuxArm64()\n")

            // Check that platform libraries are correctly generated for both root project and a subproject.
            buildWithLightDist("assemble") {
                assertOutputContains("Kotlin/Native distribution: .*kotlin-native-$platformName".toRegex())
                assertOutputContains("Generate platform libraries for linux_x64")
                assertOutputContains("Generate platform libraries for linux_arm64")
            }

            // Check that we don't generate libraries during a second run. Don't clean to reduce execution time.
            buildWithLightDist("assemble") {
                assertOutputDoesNotContain("Generate platform libraries for ")
            }
        }
    }

    @DisplayName("Link with args via gradle properties")
    @GradleTest
    fun testLinkerArgsViaGradleProperties(gradleVersion: GradleVersion) {
        nativeProject("native-platform-libraries", gradleVersion = gradleVersion) {

            addPropertyToGradleProperties(
                "kotlin.native.linkArgs",
                mapOf(
                    "-Xfoo" to "-Xfoo=bar",
                    "-Xbaz" to "-Xbaz=qux"
                )
            )

            buildGradleKts.appendText(
                """
                |
                |kotlin.linuxX64() {
                |    binaries.sharedLib {
                |        freeCompilerArgs += "-Xmen=pool"
                |    }
                |}
                """.trimMargin()
            )

            build("linkDebugSharedLinuxX64") {
                assertTasksExecuted(
                    ":compileKotlinLinuxX64",
                    ":linkDebugSharedLinuxX64"
                )
                extractNativeTasksCommandLineArgumentsFromOutput(":linkDebugSharedLinuxX64") {
                    assertCommandLineArgumentsContain("-Xfoo=bar", "-Xbaz=qux", "-Xmen=pool")
                }
                assertFileInProjectExists("build/bin/linuxX64/debugShared/libnative_platform_libraries.so")
                assertFileInProjectExists("build/bin/linuxX64/debugShared/libnative_platform_libraries_api.h")
            }
        }
    }

    @OsCondition(supportedOn = [OS.LINUX], enabledOnCI = [OS.LINUX])
    @DisplayName("Assembling project generates no platform libraries for unsupported host")
    @GradleTest
    fun testNoGenerationForUnsupportedHost(gradleVersion: GradleVersion) {
        platformLibrariesProject(KonanTarget.IOS_X64.presetName, gradleVersion = gradleVersion) {
            buildWithLightDist("assemble") {
                assertOutputDoesNotContain("Generate platform libraries for ")
            }
        }
    }

    @DisplayName("Build K/N project with prebuild type")
    @GradleTest
    fun testCanUsePrebuiltDistribution(gradleVersion: GradleVersion) {
        platformLibrariesProject("linuxX64", gradleVersion = gradleVersion) {
            build(
                "assemble", buildOptions = defaultBuildOptions.copy(
                    nativeOptions = defaultBuildOptions.nativeOptions.copy(
                        distributionType = "prebuilt"
                    )
                )
            ) {
                assertOutputContains("Kotlin/Native distribution: .*kotlin-native-prebuilt-$platformName".toRegex())
                assertOutputDoesNotContain("Generate platform libraries for ")
            }
        }
    }

    @DisplayName("Build K/N project with compiler reinstallation")
    @GradleTest
    fun testCompilerReinstallation(gradleVersion: GradleVersion) {
        platformLibrariesProject("linuxX64", gradleVersion = gradleVersion) {
            // Install the compiler at the first time. Don't build to reduce execution time.
            buildWithLightDist("tasks") {
                assertOutputContains("Generate platform libraries for linux_x64")
            }

            // Reinstall the compiler.
            buildWithLightDist(
                "tasks",
                buildOptions = defaultBuildOptions.copy(nativeOptions = defaultBuildOptions.nativeOptions.copy(reinstall = true))
            ) {
                assertOutputContains("Unpack Kotlin/Native compiler to ")
                assertOutputContains("Generate platform libraries for linux_x64")
            }
        }
    }

    @DisplayName("Download prebuilt Native bundle with maven")
    @GradleTest
    fun shouldDownloadPrebuiltNativeBundleWithMaven(gradleVersion: GradleVersion) {

        nativeProject("native-download-maven", gradleVersion = gradleVersion) {
            build(
                "assemble",
                buildOptions = defaultBuildOptions.copy(
                    nativeOptions = defaultBuildOptions.nativeOptions.copy(
                        version = TestVersions.Kotlin.STABLE_RELEASE,
                    )
                )
            ) {
                assertOutputContains("Unpack Kotlin/Native compiler to ")
                assertOutputDoesNotContain("Generate platform libraries for ")
            }
        }
    }

    @DisplayName("Download light Native bundle with maven")
    @RequiredXCodeVersion(minSupportedMajor = 14, minSupportedMinor = 1)
    @GradleTest
    fun shouldDownloadLightNativeBundleWithMaven(gradleVersion: GradleVersion) {
        nativeProject("native-download-maven", gradleVersion = gradleVersion) {
            val nativeOptions = defaultBuildOptions.nativeOptions.copy(
                distributionType = "light",
                version = TestVersions.Kotlin.STABLE_RELEASE,
            )
            build(
                "assemble",
                buildOptions = defaultBuildOptions.copy(nativeOptions = nativeOptions),
            ) {
                assertOutputContains("Unpack Kotlin/Native compiler to ")
                assertOutputContains("Generate platform libraries for ")
            }
        }
    }

    @DisplayName("Download from maven should fail if there is no such build in the default repos")
    @GradleTest
    fun shouldFailDownloadWithNoBuildInDefaultRepos(gradleVersion: GradleVersion) {
        nativeProject("native-download-maven", gradleVersion = gradleVersion) {
            val nativeOptions = BuildOptions.NativeOptions(
                version = "1.8.0-dev-1234",
                distributionDownloadFromMaven = true
            )
            buildAndFail(
                "assemble",
                buildOptions = defaultBuildOptions.copy(nativeOptions = nativeOptions)
            ) {
                assertOutputContains("Could not find org.jetbrains.kotlin:kotlin-native")
            }
        }
    }

    @DisplayName("The plugin shouldn't download the K/N compiler if there are no corresponding targets in the project.")
    @GradleTest
    fun shouldNotDownloadKonanWithoutCorrespondingTargets(gradleVersion: GradleVersion) {
        nativeProject("jvm-and-js-hmpp", gradleVersion) {
            build("tasks") {
                assertOutputDoesNotContain("Kotlin/Native distribution: ")
            }
        }
    }

    @DisplayName("The plugin shouldn't download the K/N compiler if there is konan home property override and no konan.data.dir property override.")
    @GradleTest
    fun testNativeCompilerDownloadingWithDifferentKNHomeOptions(gradleVersion: GradleVersion) {
        nativeProject("native-libraries", gradleVersion) {

            // This directory actually doesn't contain a K/N distribution
            // but we still can run a project configuration and check logs.
            val currentDir = projectPath
            build("tasks", "-Pkotlin.native.home=$currentDir", buildOptions = defaultBuildOptions.copy(konanDataDir = null)) {
                assertOutputContains("User-provided Kotlin/Native distribution: $currentDir")
                assertOutputDoesNotContain("Project property 'org.jetbrains.kotlin.native.home' is deprecated")
                assertHasDiagnostic(KotlinToolingDiagnostics.NativeStdlibIsMissingDiagnostic, withSubstring = "kotlin.native.home")
            }

            // Deprecated property.
            build(
                "tasks",
                "-Porg.jetbrains.kotlin.native.home=$currentDir",
                "-Pkotlin.native.nostdlib=true",
                buildOptions = defaultBuildOptions.copy(konanDataDir = null)
            ) {
                assertOutputContains("User-provided Kotlin/Native distribution: $currentDir")
                assertOutputContains("Project property 'org.jetbrains.kotlin.native.home' is deprecated")
                assertNoDiagnostic(KotlinToolingDiagnostics.NativeStdlibIsMissingDiagnostic)
            }
        }
    }

    @DisplayName("Checks downloading K/N compiler with different version options")
    @GradleTest
    fun testNativeCompilerDownloadingWithDifferentVersionOptions(gradleVersion: GradleVersion) {
        nativeProject("native-libraries", gradleVersion) {
            val platform = HostManager.platformName()
            build("tasks") {
                assertOutputContains("Kotlin/Native distribution:")
            }

            val version = TestVersions.Kotlin.STABLE_RELEASE
            val escapedRegexVersion = Regex.escape(TestVersions.Kotlin.STABLE_RELEASE)
            build("tasks", "-Pkotlin.native.version=$version") {
                assertOutputContains("Kotlin/Native distribution: .*kotlin-native-prebuilt-$platform-$escapedRegexVersion".toRegex())
                assertOutputDoesNotContain("Project property 'org.jetbrains.kotlin.native.version' is deprecated")
            }

            // Deprecated property
            build("tasks", "-Porg.jetbrains.kotlin.native.version=$version") {
                assertOutputContains("Kotlin/Native distribution: .*kotlin-native-prebuilt-$platform-$escapedRegexVersion".toRegex())
                assertOutputContains("Project property 'org.jetbrains.kotlin.native.version' is deprecated")
            }
        }
    }

    private fun platformLibrariesProject(
        vararg targets: String,
        gradleVersion: GradleVersion,
        test: TestProject.() -> Unit = {},
    ) {
        nativeProject("native-platform-libraries", gradleVersion) {
            buildGradleKts.appendText(
                targets.joinToString(prefix = "\n", separator = "\n") {
                    "kotlin.$it()"
                }
            )
            test()
        }
    }

    private fun TestProject.buildWithLightDist(
        vararg tasks: String,
        buildOptions: BuildOptions = defaultBuildOptions.copy(),
        assertions: BuildResult.() -> Unit,
    ) =
        build(
            *tasks,
            buildOptions = buildOptions.copy(
                nativeOptions = buildOptions.nativeOptions.copy(distributionType = "light")
            ),
            assertions = assertions
        )

}

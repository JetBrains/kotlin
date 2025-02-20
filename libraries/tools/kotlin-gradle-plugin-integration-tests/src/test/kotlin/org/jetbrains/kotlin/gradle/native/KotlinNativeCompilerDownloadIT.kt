/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.internal.os.OperatingSystem
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.TestVersions.Kotlin.STABLE_RELEASE
import org.jetbrains.kotlin.gradle.util.capitalize
import org.jetbrains.kotlin.gradle.util.replaceFirst
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.test.assertContains
import kotlin.test.fail

@OsCondition(supportedOn = [OS.MAC, OS.LINUX], enabledOnCI = [OS.LINUX]) // disabled for Windows because of tmp dir problem: KT-62761
@DisplayName("This test class contains different scenarios with downloading Kotlin Native Compiler during build.")
@NativeGradlePluginTests
class KotlinNativeCompilerDownloadIT : KGPBaseTest() {

    private val currentPlatform = HostManager.platformName()

    private val nativeHostTargetName = HostManager.host.presetName

    private val UNPUCK_KONAN_FINISHED_LOG =
        "Moving Kotlin/Native bundle from tmp directory"

    private val STABLE_VERSION_DIR_NAME = "kotlin-native-prebuilt-$currentPlatform-$STABLE_RELEASE"

    private val DOWNLOAD_KONAN_FINISHED_LOG =
        "Download $STABLE_VERSION_DIR_NAME.${if (HostManager.hostIsMingw) "zip" else "tar.gz"} finished,"

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            nativeOptions = super.defaultBuildOptions.nativeOptions.copy(
                version = STABLE_RELEASE,
                distributionDownloadFromMaven = true
            )
        )

    @DisplayName("KT-58303: Kotlin Native must not be downloaded during configuration phase")
    @GradleTest
    fun shouldNotDownloadKotlinNativeOnConfigurationPhase(gradleVersion: GradleVersion, @TempDir konanTemp: Path) {
        nativeProject(
            "native-simple-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                konanDataDir = konanTemp,
            ),
        ) {
            build("help") {
                assertOutputDoesNotContain(DOWNLOAD_KONAN_FINISHED_LOG)
                assertOutputDoesNotContain(UNPUCK_KONAN_FINISHED_LOG)
                assertOutputDoesNotContain("Please wait while Kotlin/Native")
            }
        }
    }

    @DisplayName("KT-58303: Kotlin Native must not be downloaded when konan home is overridden")
    @GradleTest
    fun shouldNotDownloadKotlinNativeWithCustomKonanHome(gradleVersion: GradleVersion) {
        val kotlinNativeVersion = System.getProperty("kotlinNativeVersion")
        // When a Kotlin Native version has not been passed means that there was not common test directory with a preinstalled kotlin native bundle
        if (kotlinNativeVersion != null) {
            val bundleDirName = "kotlin-native-prebuilt-$currentPlatform-$kotlinNativeVersion"
            val customKonanHome = konanDir.resolve(bundleDirName)
            nativeProject(
                "native-simple-project",
                gradleVersion,
                buildOptions = defaultBuildOptions.copy(
                    freeArgs = listOf("-Pkotlin.native.home=$customKonanHome"),
                    // when both `konan.data.dir` and `kotlin.native.home` properties are set the `konan.data.dir` one has priority,
                    // that is why we are disabling it `konan.data.dir` here.
                    konanDataDir = null
                ),
            ) {
                build(":commonizeNativeDistribution") {
                    assertOutputContains("A user-provided Kotlin/Native distribution configured: ${customKonanHome}. Disabling Kotlin Native Toolchain auto-provisioning.")
                    assertOutputDoesNotContain(UNPUCK_KONAN_FINISHED_LOG)
                    assertOutputDoesNotContain("Please wait while Kotlin/Native")
                }
            }
        }
    }

    @DisplayName("KT-58303: Kotlin Native must be downloaded during execution phase")
    @GradleTest
    fun shouldDownloadKotlinNativeOnExecutionPhase(gradleVersion: GradleVersion, @TempDir konanTemp: Path) {
        nativeProject(
            "native-simple-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                konanDataDir = konanTemp,
            ),
        ) {
            build("assemble") {
                assertOutputDoesNotContain(DOWNLOAD_KONAN_FINISHED_LOG)
                assertOutputContains(UNPUCK_KONAN_FINISHED_LOG)
                assertOutputDoesNotContain("Please wait while Kotlin/Native")
                assertFileExists(konanTemp.resolve(STABLE_VERSION_DIR_NAME).resolve("provisioned.ok"))
            }
        }
    }

    @DisplayName(
        "KT-65222, KT-65347: check that commonize native distribution depends on downloaded k/n bundle " +
                "and downloaded bundle does not erase installed caches"
    )
    @GradleTest
    fun checkCommonizeNativeDistributionWithPlatform(gradleVersion: GradleVersion, @TempDir konanTemp: Path) {
        nativeProject(
            "commonize-native-distribution",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                konanDataDir = konanTemp,
            ),
        ) {
            build(":compileNativeMainKotlinMetadata") {
                assertDirectoryExists(konanTemp.resolve(STABLE_VERSION_DIR_NAME).resolve("klib").resolve("platform"))
                assertDirectoryExists(konanTemp.resolve(STABLE_VERSION_DIR_NAME).resolve("bin"))
            }
        }
    }

    @DisplayName("KT-65617: check that `addKotlinNativeBundleConfiguration` does not configure dependencies after configuration has been resolved")
    @GradleTest
    fun checkThatKonanConfigurationCouldBeConfiguredOnlyOnce(gradleVersion: GradleVersion, @TempDir konanTemp: Path) {
        nativeProject(
            "commonize-native-distribution",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                konanDataDir = konanTemp,
            ),
        ) {
            buildGradleKts.replaceFirst(
                "plugins {",
                """
                @file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
                import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeBundleArtifactFormat
                plugins {
                """.trimIndent()
            )
            buildGradleKts.appendText(
                """
                    
                tasks.create("taskWithConfigurationResolvedConfiguration") {
                    dependsOn(":commonizeNativeDistribution")
                    notCompatibleWithConfigurationCache("Passing the project is not compatible with the configuration cache")
                    doFirst {
                        KotlinNativeBundleArtifactFormat.addKotlinNativeBundleConfiguration(project.rootProject)
                    }
                }
                """.trimIndent()
            )
            build(":commonizeNativeDistribution", "taskWithConfigurationResolvedConfiguration")
        }
    }

    @DisplayName("KT-58303: Downloading Kotlin Native on configuration phase(deprecated version)")
    @GradleTest
    fun shouldDownloadKotlinNativeOnConfigurationPhaseWithToolchainDisabled(gradleVersion: GradleVersion, @TempDir konanTemp: Path) {
        nativeProject(
            "native-simple-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                konanDataDir = konanTemp,
                freeArgs = listOf("-Pkotlin.native.toolchain.enabled=false"),
            ),
        ) {
            build("assemble") {
                assertOutputContains(DOWNLOAD_KONAN_FINISHED_LOG)
                assertOutputContains("Please wait while Kotlin/Native")
            }
        }
    }

    @DisplayName("Checking multi-module native project with different path to kotlin native compiler in each module")
    @GradleTest
    fun multiModuleProjectWithDifferentKotlinNativeCompilers(gradleVersion: GradleVersion, @TempDir customNativeHomePath: Path) {
        nativeProject("native-multi-module-project", gradleVersion, configureSubProjects = true) {
            val defaultKotlinNativeHomePath =
                defaultBuildOptions.konanDataDir?.toAbsolutePath()?.normalize()
                    ?: error("Default konan data dir must be set in this test before overriding")

            // setting different konan home in different subprojects
            subProject("native1").gradleProperties.appendKonanToGradleProperties(customNativeHomePath.absolutePathString())
            subProject("native2").gradleProperties.appendKonanToGradleProperties(defaultKotlinNativeHomePath.absolutePathString())

            val buildOptions = defaultBuildOptions.copy(
                konanDataDir = null,
            )

            build("assemble", buildOptions = buildOptions) {

                // check that in first project we use k/n from custom konan location
                assertNativeTasksClasspath(":native1:compileKotlin${nativeHostTargetName.capitalize()}") {
                    val konanLibsPath = customNativeHomePath.resolve(STABLE_VERSION_DIR_NAME).resolve("konan").resolve("lib")
                    assertContains(it, konanLibsPath.resolve("kotlin-native-compiler-embeddable.jar").absolutePathString())
                }

                // check that in second project we use k/n from default konan location
                assertNativeTasksClasspath(":native2:compileKotlin${nativeHostTargetName.capitalize()}") {
                    val konanLibsPath = defaultKotlinNativeHomePath.resolve(STABLE_VERSION_DIR_NAME).resolve("konan").resolve("lib")
                    assertContains(it, konanLibsPath.resolve("kotlin-native-compiler-embeddable.jar").absolutePathString())
                }
            }
        }
    }

    @DisplayName("KT-71051: Kotlin Native should be download only ones for multi-module project")
    @GradleTest
    fun shouldDownloadKotlinNativeOnlyOnes(gradleVersion: GradleVersion, @TempDir konanTemp: Path) {
        nativeProject(
            "native-multi-module-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                konanDataDir = konanTemp,
            ),
            configureSubProjects = true,
        ) {
            val os = OperatingSystem.current()
            val expectedNativeDependencies = when {
                os.isMacOsX -> listOf("lldb-4-macos", "apple-llvm-20200714-macos-aarch64-essentials", "libffi-3.3-1-macos-arm64")
                os.isLinux -> listOf("x86_64-unknown-linux-gnu-gcc-8.3.0-glibc-2.19-kernel-4.9-2", "lldb-4-linux", "llvm-11.1.0-linux-x64-essentials", "libffi-3.2.1-2-linux-x86-64")
                os.isWindows -> listOf("llvm-11.1.0-windows-x64-essentials", "libffi-3.3-windows-x64-1", "lldb-2-windows", "lld-12.0.1-windows-x64", "msys2-mingw-w64-x86_64-2")
                else -> fail("Unsupported os: ${os.name}")
            }

            build("assemble") {
                assertOutputContainsExactlyTimes("Extracting dependency:", expectedNativeDependencies.size)
                assertOutputContainsExactlyTimes("Downloading dependency https://download.jetbrains.com/kotlin/native/", expectedNativeDependencies.size)
            }
        }
    }

    private fun Path.appendKonanToGradleProperties(konanAbsolutePathString: String) {
        this.appendText(
            """
                    
            konan.data.dir=${konanAbsolutePathString}
            """.trimIndent()
        )
    }
}
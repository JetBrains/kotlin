/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.embedProject
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test

class NativePlatformLibsIT : BaseGradleIT() {

    companion object {
        @BeforeClass
        @JvmStatic
        fun skipOnWindows() {
            // This test class causes build timeouts on Windows CI machines.
            // We temporary disable it for windows until a proper fix is found.
            Assume.assumeFalse(HostManager.hostIsMingw)
        }
    }

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    private val oldCompilerVersion = "1.3.61"
    private val currentCompilerVersion = NativeCompilerDownloader.DEFAULT_KONAN_VERSION

    private fun platformLibrariesProject(vararg targets: String): Project =
        transformProjectWithPluginsDsl("native-platform-libraries").apply {
            setupWorkingDir()
            gradleBuildScript().appendText(
                targets.joinToString(prefix = "\n", separator = "\n") {
                    "kotlin.$it()"
                }
            )
            configureMemoryInGradleProperties()
        }

    private fun deleteInstalledCompilers() {
        // Clean existing installation directories.
        val osName = HostManager.simpleOsName()
        val oldCompilerDir = DependencyDirectories.localKonanDir.resolve("kotlin-native-$osName-$oldCompilerVersion")
        val currentCompilerDir = DependencyDirectories.localKonanDir.resolve("kotlin-native-$osName-$currentCompilerVersion")

        for (compilerDirectory in listOf(oldCompilerDir, currentCompilerDir)) {
            compilerDirectory.deleteRecursively()
        }
    }

    private fun Project.buildWithLightDist(vararg tasks: String, check: CompiledProject.() -> Unit) =
        build(*tasks, "-Pkotlin.native.distribution.type=light", check = check)

    @Test
    fun testNoGenerationForOldCompiler() = with(platformLibrariesProject("linuxX64")) {
        deleteInstalledCompilers()

        // Check that we don't run the library generator for old compiler distributions where libraries are prebuilt.
        // Don't run the build to reduce execution time.
        build("tasks", "-Pkotlin.native.version=$oldCompilerVersion") {
            assertSuccessful()
            assertContains("Unpack Kotlin/Native compiler to ")
            assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-(macos|linux|windows)".toRegex())
            assertNotContains("Generate platform libraries for ")
        }
    }

    @Test
    fun testNoGenerationByDefault() = with(platformLibrariesProject("linuxX64")) {
        deleteInstalledCompilers()

        // Check that a prebuilt distribution is used by default.
        build("assemble") {
            assertSuccessful()
            assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-prebuilt-(macos|linux|windows)".toRegex())
            assertNotContains("Generate platform libraries for ")
        }
    }

    @Test
    fun testLibrariesGeneration() {
        deleteInstalledCompilers()

        val rootProject = Project("native-platform-libraries").apply {
            embedProject(Project("native-platform-libraries"), renameTo = "subproject")
            configureMemoryInGradleProperties()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

            gradleBuildScript().appendText("\nkotlin.linuxX64()\n")
            gradleBuildScript("subproject").appendText("\nkotlin.linuxArm64()\n")
        }

        with(rootProject) {
            // Check that platform libraries are correctly generated for both root project and a subproject.
            buildWithLightDist("assemble") {
                assertSuccessful()
                assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-(macos|linux|windows)".toRegex())
                assertContains("Generate platform libraries for linux_x64")
                assertContains("Generate platform libraries for linux_arm64")
            }

            // Check that we don't generate libraries during a second run. Don't clean to reduce execution time.
            buildWithLightDist("assemble") {
                assertSuccessful()
                assertNotContains("Generate platform libraries for ")
            }
        }
    }

    @Test
    fun testNoGenerationForUnsupportedHost() {
        deleteInstalledCompilers()

        val unsupportedTarget = when {
            HostManager.hostIsMac -> KonanTarget.MINGW_X64
            else -> KonanTarget.IOS_X64
        }

        platformLibrariesProject(unsupportedTarget.presetName).buildWithLightDist("assemble") {
            assertSuccessful()
            assertNotContains("Generate platform libraries for ")
        }
    }

    @Test
    fun testRerunGeneratorIfCacheKindChanged() {
        // Currently we can generate caches only for macos_x64 and ios_x64.
        Assume.assumeTrue(HostManager.hostIsMac)

        deleteInstalledCompilers()

        with(platformLibrariesProject("iosX64")) {
            // Build Mac libraries without caches.
            buildWithLightDist("tasks") {
                assertSuccessful()
                assertContains("Generate platform libraries for ios_x64")
            }

            // Change cache kind and check that platform libraries generator was executed.
            buildWithLightDist("tasks", "-Pkotlin.native.cacheKind=static") {
                assertSuccessful()
                assertContains("Precompile platform libraries for ios_x64 (precompilation: static)")
            }
        }
    }

    @Test
    fun testCanUsePrebuiltDistribution() = with(platformLibrariesProject("linuxX64")) {
        deleteInstalledCompilers()

        build("assemble", "-Pkotlin.native.distribution.type=prebuilt") {
            assertSuccessful()
            assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-prebuilt-(macos|linux|windows)".toRegex())
            assertNotContains("Generate platform libraries for ")
        }
    }

    @Test
    fun testDeprecatedRestrictedDistributionProperty() = with(platformLibrariesProject("linuxX64")) {
        build("tasks", "-Pkotlin.native.restrictedDistribution=true", "-Pkotlin.native.version=$oldCompilerVersion") {
            assertSuccessful()
            assertContains("Warning: Project property 'kotlin.native.restrictedDistribution' is deprecated. Please use 'kotlin.native.distribution.type=light' instead")

            // Restricted distribution is available for Mac hosts only.
            if (HostManager.hostIsMac) {
                assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-restricted-(macos|linux|windows)".toRegex())
            } else {
                assertNotContains("Kotlin/Native distribution: .*kotlin-native-restricted-(macos|linux|windows)".toRegex())
            }
        }

        // We allow using this deprecated property for 1.4 too. Just download the distribution without platform libs in this case.
        build("tasks", "-Pkotlin.native.restrictedDistribution=true") {
            assertSuccessful()
            assertContains("Warning: Project property 'kotlin.native.restrictedDistribution' is deprecated. Please use 'kotlin.native.distribution.type=light' instead")
            assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-(macos|linux|windows)".toRegex())
        }
    }

    @Test
    fun testSettingDistributionTypeForOldCompiler() = with(platformLibrariesProject("linuxX64")) {
        build("tasks", "-Pkotlin.native.distribution.type=prebuilt", "-Pkotlin.native.version=$oldCompilerVersion") {
            assertSuccessful()
            assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-(macos|linux|windows)".toRegex())
        }

        build("tasks", "-Pkotlin.native.distribution.type=light", "-Pkotlin.native.version=$oldCompilerVersion") {
            assertSuccessful()

            // Restricted distribution is available for Mac hosts only.
            if (HostManager.hostIsMac) {
                assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-restricted-(macos|linux|windows)".toRegex())
            } else {
                assertNotContains("Kotlin/Native distribution: .*kotlin-native-restricted-(macos|linux|windows)".toRegex())
            }
        }
    }

    @Test
    fun testSettingGenerationMode() = with(platformLibrariesProject("linuxX64")) {
        deleteInstalledCompilers()

        // Check that user can change generation mode used by the cinterop tool.
        buildWithLightDist("tasks", "-Pkotlin.native.platform.libraries.mode=metadata") {
            assertSuccessful()
            assertContainsRegex("Run tool: \"generatePlatformLibraries\" with args: .* -mode metadata".toRegex())
        }
    }

    @Test
    fun testCompilerReinstallation() = with(platformLibrariesProject("linuxX64")) {
        deleteInstalledCompilers()

        // Install the compiler at the first time. Don't build to reduce execution time.
        buildWithLightDist("tasks") {
            assertSuccessful()
            assertContains("Generate platform libraries for linux_x64")
        }

        // Reinstall the compiler.
        buildWithLightDist("tasks", "-Pkotlin.native.reinstall=true") {
            assertSuccessful()
            assertContains("Unpack Kotlin/Native compiler to ")
            assertContains("Generate platform libraries for linux_x64")
        }
    }
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.embedProject
import org.jetbrains.kotlin.gradle.native.GeneralNativeIT.Companion.containsSequentially
import org.jetbrains.kotlin.gradle.native.GeneralNativeIT.Companion.extractNativeCommandLineArguments
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.CompilerVersionImpl
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertTrue

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
            gradleProperties().apply {
                configureJvmMemory()
            }
        }

    private fun CompilerVersion.isAtLeast(compilerVersion: CompilerVersion): Boolean {
        if (this.major != compilerVersion.major) return this.major > compilerVersion.major
        if (this.minor != compilerVersion.minor) return this.minor > compilerVersion.minor
        if (this.maintenance != compilerVersion.maintenance) return this.maintenance > compilerVersion.maintenance
        if (this.meta.ordinal != compilerVersion.meta.ordinal) return this.meta.ordinal > compilerVersion.meta.ordinal
        return this.build >= compilerVersion.build
    }

    private fun simpleOsName(compilerVersion: CompilerVersion): String =
        if (compilerVersion.isAtLeast(CompilerVersionImpl(major = 1, minor = 5, maintenance = 30, build = 1466))) {
            HostManager.platformName()
        } else {
            HostManager.simpleOsName()
        }

    private fun deleteInstalledCompilers() {
        // Clean existing installation directories.
        val oldOsName = simpleOsName(CompilerVersion.fromString(oldCompilerVersion))
        val currentOsName = simpleOsName(currentCompilerVersion)
        val oldCompilerDir = DependencyDirectories.localKonanDir.resolve("kotlin-native-$oldOsName-$oldCompilerVersion")
        val currentCompilerDir = DependencyDirectories.localKonanDir.resolve("kotlin-native-$currentOsName-$currentCompilerVersion")

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
            val osName = simpleOsName(CompilerVersion.fromString(oldCompilerVersion))
            assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-$osName".toRegex())
            assertNotContains("Generate platform libraries for ")
        }
    }

    @Test
    fun testNoGenerationByDefault() = with(platformLibrariesProject("linuxX64")) {
        deleteInstalledCompilers()

        // Check that a prebuilt distribution is used by default.
        build("assemble") {
            assertSuccessful()
            val osName = simpleOsName(currentCompilerVersion)
            assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-prebuilt-$osName".toRegex())
            assertNotContains("Generate platform libraries for ")
        }
    }

    @Test
    fun testLibrariesGeneration() {
        deleteInstalledCompilers()

        val rootProject = Project("native-platform-libraries").apply {
            embedProject(Project("native-platform-libraries"), renameTo = "subproject")
            gradleProperties().apply {
                configureJvmMemory()
            }
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

            gradleBuildScript().appendText("\nkotlin.linuxX64()\n")
            gradleBuildScript("subproject").appendText("\nkotlin.linuxArm64()\n")
        }

        with(rootProject) {
            // Check that platform libraries are correctly generated for both root project and a subproject.
            buildWithLightDist("assemble") {
                assertSuccessful()
                val osName = simpleOsName(currentCompilerVersion)
                assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-$osName".toRegex())
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
        // There are no cacheable targets on MinGW for now.
        Assume.assumeFalse(HostManager.hostIsMingw)

        deleteInstalledCompilers()

        fun buildPlatformLibrariesWithoutAndWithCaches(target: KonanTarget) {
            val presetName = target.presetName
            val targetName = target.name
            with(platformLibrariesProject(presetName)) {
                // Build libraries without caches.
                buildWithLightDist("tasks") {
                    assertSuccessful()
                    assertContains("Generate platform libraries for $targetName")
                }

                // Change cache kind and check that platform libraries generator was executed.
                buildWithLightDist("tasks", "-Pkotlin.native.cacheKind.$presetName=static") {
                    assertSuccessful()
                    assertContains("Precompile platform libraries for $targetName (precompilation: static)")
                }
            }
        }
        when {
            HostManager.hostIsMac -> buildPlatformLibrariesWithoutAndWithCaches(KonanTarget.IOS_X64)
            HostManager.hostIsLinux -> buildPlatformLibrariesWithoutAndWithCaches(KonanTarget.LINUX_X64)
        }
    }

    @Test
    fun testCanUsePrebuiltDistribution() = with(platformLibrariesProject("linuxX64")) {
        deleteInstalledCompilers()

        build("assemble", "-Pkotlin.native.distribution.type=prebuilt") {
            assertSuccessful()
            val osName = simpleOsName(currentCompilerVersion)
            assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-prebuilt-$osName".toRegex())
            assertNotContains("Generate platform libraries for ")
        }
    }

    @Test
    fun testDeprecatedRestrictedDistributionProperty() = with(platformLibrariesProject("linuxX64")) {
        build("tasks", "-Pkotlin.native.restrictedDistribution=true", "-Pkotlin.native.version=$oldCompilerVersion") {
            assertSuccessful()
            assertContains("Warning: Project property 'kotlin.native.restrictedDistribution' is deprecated. Please use 'kotlin.native.distribution.type=light' instead")

            val osName = simpleOsName(currentCompilerVersion)
            val regex = "Kotlin/Native distribution: .*kotlin-native-restricted-$osName".toRegex()
            // Restricted distribution is available for Mac hosts only.
            if (HostManager.hostIsMac) {
                assertContainsRegex(regex)
            } else {
                assertNotContains(regex)
            }
        }

        // We allow using this deprecated property for 1.4 too. Just download the distribution without platform libs in this case.
        build("tasks", "-Pkotlin.native.restrictedDistribution=true") {
            assertSuccessful()
            val osName = simpleOsName(currentCompilerVersion)
            assertContains("Warning: Project property 'kotlin.native.restrictedDistribution' is deprecated. Please use 'kotlin.native.distribution.type=light' instead")
            assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-$osName".toRegex())
        }
    }

    @Test
    fun testSettingDistributionTypeForOldCompiler() = with(platformLibrariesProject("linuxX64")) {
        build("tasks", "-Pkotlin.native.distribution.type=prebuilt", "-Pkotlin.native.version=$oldCompilerVersion") {
            assertSuccessful()
            val osName = simpleOsName(CompilerVersion.fromString(oldCompilerVersion))
            val regex = "Kotlin/Native distribution: .*kotlin-native-$osName".toRegex()
            assertContainsRegex(regex)
        }

        build("tasks", "-Pkotlin.native.distribution.type=light", "-Pkotlin.native.version=$oldCompilerVersion") {
            assertSuccessful()

            val osName = simpleOsName(CompilerVersion.fromString(oldCompilerVersion))
            val regex = "Kotlin/Native distribution: .*kotlin-native-restricted-$osName".toRegex()
            // Restricted distribution is available for Mac hosts only.
            if (HostManager.hostIsMac) {
                assertContainsRegex(regex)
            } else {
                assertNotContains(regex)
            }
        }
    }

    @Test
    fun testSettingGenerationMode() = with(platformLibrariesProject("linuxX64")) {
        deleteInstalledCompilers()

        // Check that user can change generation mode used by the cinterop tool.
        buildWithLightDist("tasks", "-Pkotlin.native.platform.libraries.mode=metadata") {
            assertSuccessful()
            assertTrue(extractNativeCommandLineArguments(toolName = "generatePlatformLibraries").containsSequentially("-mode", "metadata"))
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

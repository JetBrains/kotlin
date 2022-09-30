/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.embedProject
import org.jetbrains.kotlin.gradle.native.GeneralNativeIT.Companion.containsSequentially
import org.jetbrains.kotlin.gradle.native.GeneralNativeIT.Companion.extractNativeCommandLineArguments
import org.jetbrains.kotlin.gradle.native.GeneralNativeIT.Companion.withNativeCommandLineArguments
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.CompilerVersionImpl
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.isAtLeast
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.BeforeTest

class NativeDownloadAndPlatformLibsIT : BaseGradleIT() {

    companion object {
        @BeforeClass
        @JvmStatic
        fun skipOnWindows() {
            // This test class causes build timeouts on Windows CI machines.
            // We temporarily disable it for windows until a proper fix is found.
            Assume.assumeFalse(HostManager.hostIsMingw)
        }

        private const val KOTLIN_SPACE_DEV = "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev"
        private const val MAVEN_CENTRAL = "https://cache-redirector.jetbrains.com/maven-central"
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

    private fun simpleOsName(compilerVersion: CompilerVersion): String =
        if (compilerVersion.isAtLeast(CompilerVersionImpl(major = 1, minor = 5, maintenance = 30, build = 1466))) {
            HostManager.platformName()
        } else {
            HostManager.simpleOsName()
        }

    @BeforeTest
    fun deleteInstalledCompilers() {
        val oldOsName = simpleOsName(CompilerVersion.fromString(oldCompilerVersion))
        val currentOsName = simpleOsName(currentCompilerVersion)

        val oldCompilerDir = DependencyDirectories.localKonanDir
            .resolve("kotlin-native-$oldOsName-$oldCompilerVersion")
        val currentCompilerDir = DependencyDirectories.localKonanDir
            .resolve("kotlin-native-$currentOsName-$currentCompilerVersion")
        val prebuiltDistDir = DependencyDirectories.localKonanDir
            .resolve("kotlin-native-prebuilt-$currentOsName-$currentCompilerVersion")

        for (compilerDirectory in listOf(oldCompilerDir, currentCompilerDir, prebuiltDistDir)) {
            compilerDirectory.deleteRecursively()
        }
    }

    private fun Project.buildWithLightDist(vararg tasks: String, check: CompiledProject.() -> Unit) =
        build(*tasks, "-Pkotlin.native.distribution.type=light", check = check)

    @Test
    fun testNoGenerationForOldCompiler() = with(platformLibrariesProject("linuxX64")) {
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
    fun testLinkerArgsViaGradleProperties() {
        with(Project("native-platform-libraries")) {
            setupWorkingDir()
            gradleProperties().apply {
                configureJvmMemory()
                appendText("\nkotlin.native.linkArgs=-Xfoo=bar -Xbaz=qux")
            }
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            gradleBuildScript().appendText(
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
                assertSuccessful()
                assertTasksExecuted(
                    ":compileKotlinLinuxX64",
                    ":linkDebugSharedLinuxX64"
                )
                withNativeCommandLineArguments(":linkDebugSharedLinuxX64") {
                    assertTrue(it.contains("-Xfoo=bar"))
                    assertTrue(it.contains("-Xbaz=qux"))
                    assertTrue(it.contains("-Xmen=pool"))
                }
                assertFileExists("/build/bin/linuxX64/debugShared/libnative_platform_libraries.so")
                assertFileExists("/build/bin/linuxX64/debugShared/libnative_platform_libraries_api.h")
            }
        }
    }

    @Test
    fun testNoGenerationForUnsupportedHost() {
        hostHaveUnsupportedTarget()

        platformLibrariesProject(KonanTarget.IOS_X64.presetName).buildWithLightDist("assemble") {
            assertSuccessful()
            assertNotContains("Generate platform libraries for ")
        }
    }

    @Test
    fun testRerunGeneratorIfCacheKindChanged() {
        // There are no cacheable targets on MinGW for now.
        Assume.assumeFalse(HostManager.hostIsMingw)

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
            HostManager.host == KonanTarget.MACOS_ARM64 -> buildPlatformLibrariesWithoutAndWithCaches(KonanTarget.IOS_ARM64)
            HostManager.host == KonanTarget.MACOS_X64 -> buildPlatformLibrariesWithoutAndWithCaches(KonanTarget.IOS_X64)
            HostManager.hostIsLinux -> buildPlatformLibrariesWithoutAndWithCaches(KonanTarget.LINUX_X64)
        }
    }

    @Test
    fun testCanUsePrebuiltDistribution() = with(platformLibrariesProject("linuxX64")) {
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

            val osName = simpleOsName(CompilerVersion.fromString(oldCompilerVersion))
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
        // Check that user can change generation mode used by the cinterop tool.
        buildWithLightDist("tasks", "-Pkotlin.native.platform.libraries.mode=metadata") {
            assertSuccessful()
            assertTrue(extractNativeCommandLineArguments(toolName = "generatePlatformLibraries").containsSequentially("-mode", "metadata"))
        }
    }

    @Test
    fun testCompilerReinstallation() = with(platformLibrariesProject("linuxX64")) {
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

    private fun mavenUrl(): String = when (currentCompilerVersion.meta) {
        MetaVersion.DEV -> KOTLIN_SPACE_DEV
        MetaVersion.RELEASE, MetaVersion.RC, MetaVersion("RC2"), MetaVersion.BETA -> MAVEN_CENTRAL
        else -> throw IllegalStateException("Not a published version $currentCompilerVersion")
    }

    @Test
    fun `download prebuilt Native bundle with maven`() {
        with(transformNativeTestProjectWithPluginDsl("native-download-maven")) {
            gradleProperties().appendText(
                "kotlin.native.distribution.downloadFromMaven=true"
            )
            gradleBuildScript().let {
                val text = it.readText().replaceFirst("// <MavenPlaceholder>", "maven(\"${mavenUrl()}\")")
                it.writeText(text)
            }
            build("assemble") {
                assertSuccessful()
                assertContains("Unpack Kotlin/Native compiler to ")
                assertNotContains("Generate platform libraries for ")
            }
        }
    }

    @Test
    fun `download light Native bundle with maven`() {
        with(transformNativeTestProjectWithPluginDsl("native-download-maven")) {
            gradleProperties().appendText(
                "kotlin.native.distribution.downloadFromMaven=true"
            )
            gradleBuildScript().let {
                val text = it.readText().replaceFirst("// <MavenPlaceholder>", "maven(\"${mavenUrl()}\")")
                it.writeText(text)
            }
            build("assemble", "-Pkotlin.native.distribution.type=light") {
                assertSuccessful()
                assertContains("Unpack Kotlin/Native compiler to ")
                assertContains("Generate platform libraries for ")
            }
        }
    }

    @Test
    fun `download from maven should fail if there is no such build in the default repos`() {
        with(transformNativeTestProjectWithPluginDsl("native-download-maven")) {
            gradleProperties().appendText(
                """
                    kotlin.native.version=1.8.0-dev-1234
                    kotlin.native.distribution.downloadFromMaven=true
                """.trimIndent()
            )
            build("assemble") {
                assertContains("Could not find org.jetbrains.kotlin:kotlin-native")
                assertFailed()
            }
        }
    }
}

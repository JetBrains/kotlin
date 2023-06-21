/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.embedProject
import org.jetbrains.kotlin.gradle.native.GeneralNativeIT.Companion.containsSequentially
import org.jetbrains.kotlin.gradle.native.GeneralNativeIT.Companion.withNativeCommandLineArguments
import org.jetbrains.kotlin.gradle.testbase.NativeToolKind
import org.jetbrains.kotlin.gradle.testbase.extractNativeCompilerCommandLineArguments
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.Xcode
import org.jetbrains.kotlin.konan.target.XcodeVersion
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

    private val platformName: String = HostManager.platformName()

    @BeforeTest
    fun deleteInstalledCompilers() {
        val currentCompilerDir = DependencyDirectories.localKonanDir
            .resolve("kotlin-native-$platformName-$currentCompilerVersion")
        val prebuiltDistDir = DependencyDirectories.localKonanDir
            .resolve("kotlin-native-prebuilt-$platformName-$currentCompilerVersion")

        for (compilerDirectory in listOf(currentCompilerDir, prebuiltDistDir)) {
            compilerDirectory.deleteRecursively()
        }
    }

    private fun Project.buildWithLightDist(vararg tasks: String, check: CompiledProject.() -> Unit) =
        build(*tasks, "-Pkotlin.native.distribution.type=light", check = check)

    @Test
    fun testNoGenerationByDefault() = with(platformLibrariesProject("linuxX64")) {


        // Check that a prebuilt distribution is used by default.
        build("assemble") {
            assertSuccessful()
            assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-prebuilt-$platformName".toRegex())
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
                assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-$platformName".toRegex())
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
    fun testCanUsePrebuiltDistribution() = with(platformLibrariesProject("linuxX64")) {
        build("assemble", "-Pkotlin.native.distribution.type=prebuilt") {
            assertSuccessful()
            assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-prebuilt-$platformName".toRegex())
            assertNotContains("Generate platform libraries for ")
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

    private fun mavenUrl(): String {
        val versionPattern = "(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-(\\p{Alpha}*\\p{Alnum}|[\\p{Alpha}-]*))?(?:-(\\d+))?".toRegex()
        val (_, _, _, metaString, build) = versionPattern.matchEntire(currentCompilerVersion)?.destructured
            ?: error("Unable to parse version $currentCompilerVersion")
        return when {
            metaString == "dev" || build.isNotEmpty() -> KOTLIN_SPACE_DEV
            metaString in listOf("RC", "RC2", "Beta") || metaString.isEmpty() -> MAVEN_CENTRAL
            else -> throw IllegalStateException("Not a published version $currentCompilerVersion")
        }
    }

    @Test
    fun `download prebuilt Native bundle with maven`() {
        val maven = mavenUrl()
        // Don't run this test for build that are not yet published to central
        Assume.assumeTrue(maven != MAVEN_CENTRAL)

        with(transformNativeTestProjectWithPluginDsl("native-download-maven")) {
            gradleProperties().appendText(
                "kotlin.native.distribution.downloadFromMaven=true"
            )
            gradleBuildScript().let {
                val text = it.readText().replaceFirst("// <MavenPlaceholder>", "maven(\"${maven}\")")
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
        val maven = mavenUrl()
        // Don't run this test for build that are not yet published to central
        Assume.assumeTrue(maven != MAVEN_CENTRAL)

        if (HostManager.hostIsMac) {
            // Building platform libs require Xcode 14.1
            Assume.assumeTrue(Xcode.findCurrent().version >= XcodeVersion(14, 1))
        }

        with(transformNativeTestProjectWithPluginDsl("native-download-maven")) {
            gradleProperties().appendText(
                "kotlin.native.distribution.downloadFromMaven=true"
            )
            gradleBuildScript().let {
                val text = it.readText().replaceFirst("// <MavenPlaceholder>", "maven(\"${maven}\")")
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

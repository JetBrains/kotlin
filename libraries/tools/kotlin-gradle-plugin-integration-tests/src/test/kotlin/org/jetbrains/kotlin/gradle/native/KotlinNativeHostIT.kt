/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.test.assertEquals

@DisplayName("Kotlin/Native host DSL: nativeHost and hostNativeTarget")
@NativeGradlePluginTests
class KotlinNativeHostIT : KGPBaseTest() {

    @DisplayName("hostNativeTarget declares the host target and host checks gate tasks")
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC, OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.MAC, OS.LINUX, OS.WINDOWS])
    fun testHostNativeTarget(gradleVersion: GradleVersion) {
        val host = KotlinNativeHost.current()

        nativeProject("native-host-dsl", gradleVersion) {
            build("linkDebugExecutableNative", "cinteropSampleIosArm64") {
                assertTasksExecuted(":linkDebugExecutableNative", ":cinteropSampleNative")
                assertEquals(host.name, printedValue("nativeHost.name"))
                assertEquals(host.targetName, printedValue("nativeHost.targetName"))
                assertEquals("native", printedValue("hostNativeTarget.name"))
                if (host.isMacOs) {
                    assertTasksExecuted(":cinteropSampleIosArm64")
                } else {
                    assertTasksSkipped(":cinteropSampleIosArm64")
                }
            }

            // Same tasks as the first build, so its configuration cache entry is reused.
            build("linkDebugExecutableNative", "cinteropSampleIosArm64") {
                assertConfigurationCacheReused()
            }
        }
    }

    @DisplayName("nativeHost and hostNativeTarget work from a Groovy build script")
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC, OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.MAC, OS.LINUX, OS.WINDOWS])
    fun testGroovyDsl(gradleVersion: GradleVersion) {
        val host = KotlinNativeHost.current()

        nativeProject("native-host-dsl-groovy", gradleVersion) {
            build("linkDebugExecutableNative") {
                assertTasksExecuted(":linkDebugExecutableNative")
                assertEquals(host.name, printedValue("nativeHost.name"))
                assertEquals("true", printedValue("host.supported"))
            }
        }
    }

    /** The value the build script printed as `key=value` on its own line. */
    private fun BuildResult.printedValue(key: String): String =
        output.lineSequence().single { it.startsWith("$key=") }.substringAfter("=")

    @DisplayName("HostManager still works in a build script but reports a deprecation that names KotlinNativeHost")
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC, OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.MAC, OS.LINUX, OS.WINDOWS])
    fun testHostManagerIsDeprecatedInBuildScripts(gradleVersion: GradleVersion) {
        val host = KotlinNativeHost.current()

        nativeProject("native-host-dsl", gradleVersion) {
            buildGradleKts.append(
                """
                |
                |logger.quiet("legacy.hostIsMac=" + org.jetbrains.kotlin.konan.target.HostManager.hostIsMac)
                """.trimMargin()
            )

            build("help") {
                assertEquals((host.isMacOs && host.isSupported).toString(), printedValue("legacy.hostIsMac"))
                // The class and its companion may each report the deprecation.
                val deprecationMessages = output.lineSequence()
                    .filter { it.startsWith("w: ") && "is deprecated. " in it }
                    .map { it.substringAfter("is deprecated. ") }
                    .toSet()
                assertEquals(
                    setOf("HostManager is internal to Kotlin/Native and will be hidden from build scripts in a future Kotlin release."),
                    deprecationMessages,
                )
            }
        }
    }

    @DisplayName("A plain kotlin-native-utils on the buildscript classpath next to the bundled copy does not break the build")
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC, OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.MAC, OS.LINUX, OS.WINDOWS])
    fun testPlainKotlinNativeUtilsOnBuildscriptClasspath(gradleVersion: GradleVersion) {
        nativeProject("native-host-dsl", gradleVersion) {
            buildGradleKts.modify { script ->
                script.replaceFirst(
                    "plugins {",
                    """
                    |buildscript {
                    |    repositories {
                    |        mavenLocal()
                    |        mavenCentral()
                    |    }
                    |    dependencies {
                    |        classpath("org.jetbrains.kotlin:kotlin-native-utils:${buildOptions.kotlinVersion}")
                    |    }
                    |}
                    |
                    |plugins {
                    """.trimMargin()
                )
            }

            build("linkDebugExecutableNative") {
                assertTasksExecuted(":linkDebugExecutableNative")
                assertEquals("native", printedValue("hostNativeTarget.name"))
            }
        }
    }
}

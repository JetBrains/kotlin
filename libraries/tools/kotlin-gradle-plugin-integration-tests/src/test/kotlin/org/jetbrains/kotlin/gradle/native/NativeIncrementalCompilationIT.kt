/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@OsCondition(
    // Disabled on Windows for now.
    supportedOn = [OS.LINUX, OS.MAC],
    enabledOnCI = [OS.LINUX, OS.MAC]
)
@DisplayName("Tests for K/N incremental compilation")
@NativeGradlePluginTests
class NativeIncrementalCompilationIT : KGPBaseTest() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        logLevel = LogLevel.DEBUG,
        nativeOptions = BuildOptions.NativeOptions(
            cacheKind = NativeCacheKind.STATIC,
            incremental = true
        )
    )

    @DisplayName("KT-63742: Smoke test")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_4)
    @GradleTest
    fun checkIncrementalCacheIsCreated(gradleVersion: GradleVersion) {
        nativeProject("native-incremental-simple", gradleVersion) {

            val compilerCacheOrchestrationArgs = arrayOf(
                "-Xauto-cache-from=${getGradleUserHome()}",
                "-Xbackend-threads=4"
            )

            val incrementalCacheArgs = arrayOf(
                "-Xenable-incremental-compilation",
                "-Xic-cache-dir=${projectPath.resolve("build").resolve("kotlin-native-ic-cache").toFile().canonicalPath}"
            )

            val withoutIncrementalCacheBuildOptions = defaultBuildOptions.copy(
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    incremental = false
                )
            )
            build("linkDebugExecutableHost", buildOptions = withoutIncrementalCacheBuildOptions) {
                extractNativeTasksCommandLineArgumentsFromOutput(":linkDebugExecutableHost") {
                    assertCommandLineArgumentsContain(*compilerCacheOrchestrationArgs)
                    assertCommandLineArgumentsDoNotContain(*incrementalCacheArgs)
                }
            }


            val withIncrementalCacheBuildOptions = defaultBuildOptions.copy(
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    incremental = true
                )
            )
            build("clean", "linkDebugExecutableHost", buildOptions = withIncrementalCacheBuildOptions) {
                extractNativeTasksCommandLineArgumentsFromOutput(":linkDebugExecutableHost") {
                    assertCommandLineArgumentsContain(*(compilerCacheOrchestrationArgs + incrementalCacheArgs))
                }
            }

            val withIncrementalCacheAndConfigurationCacheBuildOptions = defaultBuildOptions.copy(
                configurationCache = true,
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    incremental = true
                )
            )
            build("clean", "linkDebugExecutableHost", buildOptions = withIncrementalCacheAndConfigurationCacheBuildOptions) {
                extractNativeTasksCommandLineArgumentsFromOutput(":linkDebugExecutableHost") {
                    assertCommandLineArgumentsContain(*(compilerCacheOrchestrationArgs + incrementalCacheArgs))
                }
            }
        }
    }
}
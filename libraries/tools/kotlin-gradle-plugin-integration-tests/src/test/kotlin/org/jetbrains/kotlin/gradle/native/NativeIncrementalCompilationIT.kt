/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OsCondition(
    // Disabled on Windows for now.
    supportedOn = [OS.LINUX, OS.MAC],
    enabledOnCI = [OS.LINUX, OS.MAC]
)
@DisplayName("Tests for K/N incremental compilation")
@NativeGradlePluginTests
class NativeIncrementalCompilationIT : KGPBaseTest() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        nativeOptions = BuildOptions.NativeOptions(
            cacheKind = NativeCacheKind.STATIC,
            incremental = true
        )
    )

    @DisplayName("KT-63742: Check that kotlinNativeLink task passes all required args for cache orchestration and ic")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_4) // DefaultResolvedComponentResult with configuration cache is supported only after 7.4
    @GradleTest
    fun checkArgumentsForIncrementalCache(gradleVersion: GradleVersion) {
        nativeProject("native-incremental-simple", gradleVersion) {

            val compilerCacheOrchestrationArgs = arrayOf(
                "-Xauto-cache-from=${getGradleUserHome()}",
                "-Xbackend-threads=4"
            )

            val icCacheDir = projectPath.resolve("build").resolve("kotlin-native-ic-cache").resolve("debugExecutable")
            val incrementalCacheArgs = arrayOf(
                "-Xenable-incremental-compilation",
                "-Xic-cache-dir=${icCacheDir.toFile().canonicalPath}"
            )

            // disabled incremental cache parameter
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


            // enabled incremental cache parameter
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

            // enabled incremental cache and configuration cache parameters
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

    @DisplayName("Smoke test")
    @GradleTest
    fun checkIncrementalCacheIsCreated(gradleVersion: GradleVersion) {
        nativeProject("native-incremental-simple", gradleVersion) {
            build("linkDebugExecutableHost") {
                assertDirectoryExists(
                    getFileCache("native-incremental-simple", "src/hostMain/kotlin/main.kt")
                )
            }
        }
    }

    @DisplayName("IC works after compilation error (test 1)")
    @GradleTest
    fun compilationError1(gradleVersion: GradleVersion) {
        nativeProject("native-incremental-multifile", gradleVersion) {
            var mainKtCacheModified = 0L
            var fooKtCacheModified = 0L
            val mainKtCache = getFileCache("native-incremental-multifile", "src/hostMain/kotlin/main.kt")
            val fooKtCache = getFileCache("native-incremental-multifile", "src/hostMain/kotlin/foo.kt")
            build("linkDebugExecutableHost") {
                assertDirectoryExists(mainKtCache)
                assertDirectoryExists(fooKtCache)
                mainKtCacheModified = mainKtCache.toFile().lastModified()
                fooKtCacheModified = fooKtCache.toFile().lastModified()
            }

            val fooKt = kotlinSourcesDir("hostMain").resolve("foo.kt")
            fooKt.writeText("fun foo(): Int = \"zzz\"")

            buildAndFail("linkDebugExecutableHost") {
                assertTasksFailed(":compileKotlinHost")
            }

            fooKt.writeText("fun foo(): Int = 42")

            build("linkDebugExecutableHost") {
                assertDirectoryExists(mainKtCache)
                assertDirectoryExists(fooKtCache)
                assertEquals(mainKtCacheModified, mainKtCache.toFile().lastModified())
                assertEquals(fooKtCacheModified, fooKtCache.toFile().lastModified())
            }
        }
    }

    @DisplayName("IC works after compilation error (test 2)")
    @GradleTest
    fun compilationError2(gradleVersion: GradleVersion) {
        nativeProject("native-incremental-multifile", gradleVersion) {
            var mainKtCacheModified = 0L
            var fooKtCacheModified = 0L
            val mainKtCache = getFileCache("native-incremental-multifile", "src/hostMain/kotlin/main.kt")
            val fooKtCache = getFileCache("native-incremental-multifile", "src/hostMain/kotlin/foo.kt")
            build("linkDebugExecutableHost") {
                assertDirectoryExists(mainKtCache)
                assertDirectoryExists(fooKtCache)
                mainKtCacheModified = mainKtCache.toFile().lastModified()
                fooKtCacheModified = fooKtCache.toFile().lastModified()
            }

            val fooKt = kotlinSourcesDir("hostMain").resolve("foo.kt")
            fooKt.writeText("fun foo(): Int = \"zzz\"")

            buildAndFail("linkDebugExecutableHost") {
                assertTasksFailed(":compileKotlinHost")
            }

            fooKt.writeText("fun foo(): String = \"zzz\"")

            build("linkDebugExecutableHost") {
                assertDirectoryExists(mainKtCache)
                assertDirectoryExists(fooKtCache)
                assertNotEquals(mainKtCacheModified, mainKtCache.toFile().lastModified())
                assertNotEquals(fooKtCacheModified, fooKtCache.toFile().lastModified())
            }
        }
    }

    @DisplayName("Check dependencies on project level")
    @GradleTest
    fun inProjectDependencies(gradleVersion: GradleVersion) {
        nativeProject("native-incremental-multi-project", gradleVersion, configureSubProjects = true) {
            var fooKtCacheModified = 0L
            var barKtCacheModified = 0L
            var mainKtCacheModified = 0L
            val fooKtCache = getFileCache(
                "MultiProject:library", "library/src/hostMain/kotlin/foo.kt",
                executableProjectName = "program"
            )
            val barKtCache = getFileCache(
                "MultiProject:program", "program/src/hostMain/kotlin/bar.kt",
                executableProjectName = "program"
            )
            val mainKtCache = getFileCache(
                "MultiProject:program", "program/src/hostMain/kotlin/main.kt",
                executableProjectName = "program"
            )
            build("linkDebugExecutableHost") {
                assertDirectoryExists(fooKtCache)
                assertDirectoryExists(barKtCache)
                assertDirectoryExists(mainKtCache)
                fooKtCacheModified = fooKtCache.toFile().lastModified()
                barKtCacheModified = barKtCache.toFile().lastModified()
                mainKtCacheModified = mainKtCache.toFile().lastModified()
            }

            val fooKt = projectPath.resolve("library/src/hostMain/kotlin").resolve("foo.kt")
            fooKt.writeText("fun foo(): Int = 41")

            build("linkDebugExecutableHost") {
                assertDirectoryExists(fooKtCache)
                assertDirectoryExists(barKtCache)
                assertDirectoryExists(mainKtCache)
                assertNotEquals(fooKtCacheModified, fooKtCache.toFile().lastModified())
                assertEquals(barKtCacheModified, barKtCache.toFile().lastModified())
                assertNotEquals(mainKtCacheModified, mainKtCache.toFile().lastModified())
            }
        }
    }
}
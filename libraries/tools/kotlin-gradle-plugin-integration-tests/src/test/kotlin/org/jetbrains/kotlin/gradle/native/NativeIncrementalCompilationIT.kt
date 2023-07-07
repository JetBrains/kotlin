/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@DisplayName("Tests for K/N incremental compilation")
@NativeGradlePluginTests
class NativeIncrementalCompilationIT : KGPBaseTest() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        nativeOptions = BuildOptions.NativeOptions(
            cacheKind = NativeCacheKind.STATIC,
            incremental = true
        )
    )

    @DisplayName("Smoke test")
    @GradleTest
    fun checkIncrementalCacheIsCreated(gradleVersion: GradleVersion) {
        nativeProject("native-incremental-simple", gradleVersion) {
            build("linkDebugExecutableHost") {
                assertDirectoryExists(
                    getFileCache("native-incremental-simple", "src/hostMain/kotlin/main.kt", "")
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
            val mainKtCache = getFileCache("native-incremental-multifile", "src/hostMain/kotlin/main.kt", "")
            val fooKtCache = getFileCache("native-incremental-multifile", "src/hostMain/kotlin/foo.kt", "")
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
            val mainKtCache = getFileCache("native-incremental-multifile", "src/hostMain/kotlin/main.kt", "")
            val fooKtCache = getFileCache("native-incremental-multifile", "src/hostMain/kotlin/foo.kt", "")
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
            val fooKtCache = getFileCache("program", "MultiProject:library", "library/src/hostMain/kotlin/foo.kt", "")
            val barKtCache = getFileCache("program", "MultiProject:program", "program/src/hostMain/kotlin/bar.kt", "")
            val mainKtCache = getFileCache("program", "MultiProject:program", "program/src/hostMain/kotlin/main.kt", "")
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
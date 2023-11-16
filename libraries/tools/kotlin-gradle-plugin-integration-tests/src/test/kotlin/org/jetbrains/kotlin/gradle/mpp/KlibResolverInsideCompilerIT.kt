/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.file.file
import org.jetbrains.kotlin.konan.file.withZipFileSystem
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDENCY_VERSION
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue
import org.jetbrains.kotlin.konan.file.File as KFile

@NativeGradlePluginTests
@DisplayName("Tests for KLIB resolver inside the Kotlin compiler")
@GradleTestVersions(maxVersion = TestVersions.Gradle.G_8_2)
class KlibResolverInsideCompilerIT : KGPBaseTest() {
    @DisplayName("Test resolver with C-interop KLIBs (KT-62515)")
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0, maxVersion = TestVersions.Gradle.G_8_2)
    fun testResolverWithCInteropKlibs(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        buildProjects(
            baseDir = "mpp-klib-resolver-inside-compiler/with-cinterop",
            tempDir, gradleVersion
        )
    }

    @DisplayName("Test resolver without C-interop KLIBs (KT-62515)")
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0, maxVersion = TestVersions.Gradle.G_8_2)
    fun testResolverWithoutCInteropKlibs(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        buildProjects(
            baseDir = "mpp-klib-resolver-inside-compiler/without-cinterop",
            tempDir, gradleVersion
        )
    }

    private fun createLocalRepo(tempDir: Path): Path {
        val localRepo = tempDir.resolve("local-repo").toAbsolutePath()
        Files.createDirectories(localRepo)
        return localRepo
    }

    private fun buildProject(
        baseDir: String,
        projectName: String,
        gradleVersion: GradleVersion,
        localRepoDir: Path,
        buildTask: String = "publish",
    ) {
        project("$baseDir/$projectName", gradleVersion, localRepoDir = localRepoDir) {
            build(buildTask)
        }
    }

    private fun buildProjects(baseDir: String, tempDir: Path, gradleVersion: GradleVersion) {
        val localRepoDir = createLocalRepo(tempDir)

        buildProject(baseDir, "liba-v1", gradleVersion, localRepoDir)
        buildProject(baseDir, "liba-v2", gradleVersion, localRepoDir)
        buildProject(baseDir, "libb", gradleVersion, localRepoDir)
        buildProject(baseDir, "libc", gradleVersion, localRepoDir)

        // Make sure there are no `dependency_version_<name>=` properties with non-`unspecified` values in the manifest file:
        localRepoDir.toFile().walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "klib") {
                val specifiedDependencyVersions: Map<String, String> = KFile(file.absolutePath).withZipFileSystem {
                    it.file("default/manifest")
                        .loadProperties()
                        .entries
                        .mapNotNull { (key, value) ->
                            val keyStr = key.toString()
                            if (!keyStr.startsWith(KLIB_PROPERTY_DEPENDENCY_VERSION)) return@mapNotNull null

                            val valueStr = value.toString()
                            if (valueStr == "unspecified") return@mapNotNull null

                            keyStr to valueStr
                        }.toMap()
                }

                assertTrue(
                    specifiedDependencyVersions.isEmpty(),
                    "There are specific library versions in the manifest of the library $file:\n${specifiedDependencyVersions.entries.sortedBy { it.key }}"
                )
            }
        }

        buildProject(baseDir, "app", gradleVersion, localRepoDir, buildTask = "assemble")
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@MppGradlePluginTests
@DisplayName("Tests for KLIB resolver inside the Kotlin compiler")
class KlibResolverInsideCompilerIT : KGPBaseTest() {
    @DisplayName("KLIBs with duplicated unique_name not discriminated, library + composite build, LV=1.9 (KT-63573)")
    @GradleTest
    fun testKlibsWithDuplicatedUniqueNameNotDiscriminated1(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        buildLibraryAndCompositeProjectForKT63573(
            baseDir = "mpp-klib-resolver-inside-compiler/klibs-with-duplicated-unique_name-library-and-composite-build",
            languageVersion = EnforcedLanguageVersion.K1,
            tempDir, gradleVersion
        )
    }

    @DisplayName("KLIBs with duplicated unique_name not discriminated, library + composite build, LV=2.0 (KT-63573)")
    @GradleTest
    fun testKlibsWithDuplicatedUniqueNameNotDiscriminated2(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        buildLibraryAndCompositeProjectForKT63573(
            baseDir = "mpp-klib-resolver-inside-compiler/klibs-with-duplicated-unique_name-library-and-composite-build",
            languageVersion = EnforcedLanguageVersion.K2,
            tempDir, gradleVersion
        )
    }

    @DisplayName("KLIBs with duplicated unique_name not discriminated, library x2 + app, LV=1.9 (KT-63573)")
    @GradleTest
    fun testKlibsWithDuplicatedUniqueNameNotDiscriminated3(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        buildTwoLibrariesAndAppForKT63573(
            baseDir = "mpp-klib-resolver-inside-compiler/klibs-with-duplicated-unique_name-library-x2-and-app",
            languageVersion = EnforcedLanguageVersion.K1,
            tempDir, gradleVersion
        )
    }

    @DisplayName("KLIBs with duplicated unique_name not discriminated, library x2 + app, LV=2.0 (KT-63573)")
    @GradleTest
    fun testKlibsWithDuplicatedUniqueNameNotDiscriminated4(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        buildTwoLibrariesAndAppForKT63573(
            baseDir = "mpp-klib-resolver-inside-compiler/klibs-with-duplicated-unique_name-library-x2-and-app",
            languageVersion = EnforcedLanguageVersion.K2,
            tempDir, gradleVersion
        )
    }

    private fun createLocalRepo(tempDir: Path): Path {
        val localRepo = tempDir.resolve("local-repo").toAbsolutePath()
        Files.createDirectories(localRepo)
        return localRepo
    }

    private enum class EnforcedLanguageVersion { K1, K2 }

    private fun buildProject(
        baseDir: String,
        projectName: String,
        gradleVersion: GradleVersion,
        localRepoDir: Path,
        languageVersion: EnforcedLanguageVersion? = null,
        buildTask: String = "build",
    ) {
        val buildOptions = with(defaultBuildOptions) {
            when (languageVersion) {
                EnforcedLanguageVersion.K1 -> copyEnsuringK1()
                EnforcedLanguageVersion.K2 -> copyEnsuringK2()
                else -> this
            }
        }

        project("$baseDir/$projectName", gradleVersion, buildOptions, localRepoDir = localRepoDir) {
            build(buildTask)
        }
    }

    private fun buildAndPublishProject(
        baseDir: String,
        projectName: String,
        gradleVersion: GradleVersion,
        localRepoDir: Path
    ) = buildProject(baseDir, projectName, gradleVersion, localRepoDir, buildTask = "publish")

    private fun buildLibraryAndCompositeProjectForKT63573(
        baseDir: String,
        languageVersion: EnforcedLanguageVersion,
        tempDir: Path,
        gradleVersion: GradleVersion,
    ) {
        val localRepoDir = createLocalRepo(tempDir)

        buildAndPublishProject(baseDir, "external-library", gradleVersion, localRepoDir)
        buildProject(baseDir, "composite-project", gradleVersion, localRepoDir, languageVersion, ":app:build")
    }

    private fun buildTwoLibrariesAndAppForKT63573(
        baseDir: String,
        languageVersion: EnforcedLanguageVersion,
        tempDir: Path,
        gradleVersion: GradleVersion,
    ) {
        val localRepoDir = createLocalRepo(tempDir)

        buildAndPublishProject(baseDir, "external-library1", gradleVersion, localRepoDir)
        buildAndPublishProject(baseDir, "external-library2", gradleVersion, localRepoDir)
        buildProject(baseDir, "app", gradleVersion, localRepoDir, languageVersion, "build")
    }
}

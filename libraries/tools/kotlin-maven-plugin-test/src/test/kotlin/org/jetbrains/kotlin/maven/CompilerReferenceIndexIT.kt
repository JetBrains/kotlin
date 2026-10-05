/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Compiler reference index")
class CompilerReferenceIndexIT : KotlinMavenTestBase() {

    @MavenTest
    @DisplayName("Warns and skips CRI generation when incremental compilation is disabled")
    fun testCriRequiresIncrementalCompilation(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            setMavenProperty("kotlin.compiler.generateCompilerRefIndex", "true")
            setMavenProperty("kotlin.compiler.incremental", "false")

            build("compile") {
                assertBuildLogContains(REQUIRES_INCREMENTAL_COMPILATION_WARNING)
                assertFileNotExists(CRI_DIR)
            }
        }
    }

    @MavenTest
    @DisplayName("Generates CRI without warning when incremental compilation is enabled")
    fun testCriWithIncrementalCompilationEnabled(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            setMavenProperty("kotlin.compiler.generateCompilerRefIndex", "true")
            setMavenProperty("kotlin.compiler.incremental", "true")

            build("compile") {
                assertBuildLogContains("Using experimental Kotlin incremental compilation")
                assertBuildLogDoesNotContain(REQUIRES_INCREMENTAL_COMPILATION_WARNING)
                assertFileExists(CRI_SUBTYPES_TABLE)
            }
        }
    }

    // No test for the default outside CI: nested Maven builds inherit the CI agent's env vars, and they can't be unset

    @MavenTest
    @DisplayName("Does not warn about CRI by default when incremental compilation is disabled")
    fun testCriDefaultWithoutIncrementalCompilation(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            setMavenProperty("kotlin.compiler.incremental", "false")

            build("compile") {
                assertBuildLogDoesNotContain(REQUIRES_INCREMENTAL_COMPILATION_WARNING)
                assertFileNotExists(CRI_DIR)
            }
        }
    }

    @MavenTest
    @DisplayName("Does not generate CRI by default on CI")
    fun testCriDisabledByDefaultOnCi(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            setMavenProperty("kotlin.compiler.incremental", "true")

            build("compile", environmentVariables = mapOf("CI" to "true")) {
                assertBuildLogContains("Using experimental Kotlin incremental compilation")
                assertBuildLogDoesNotContain(REQUIRES_INCREMENTAL_COMPILATION_WARNING)
                assertFileNotExists(CRI_DIR)
            }
        }
    }

    @MavenTest
    @DisplayName("Does not generate CRI by default when CI is detected via a system property")
    fun testCriDisabledByDefaultWithCiSystemProperty(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            setMavenProperty("kotlin.compiler.incremental", "true")

            build("compile", "-DTEAMCITY_VERSION=1.0.0") {
                assertBuildLogContains("Using experimental Kotlin incremental compilation")
                assertFileNotExists(CRI_DIR)
            }
        }
    }

    @MavenTest
    @DisplayName("Generates CRI on CI when explicitly enabled")
    fun testCriExplicitlyEnabledOnCi(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            setMavenProperty("kotlin.compiler.generateCompilerRefIndex", "true")
            setMavenProperty("kotlin.compiler.incremental", "true")

            build("compile", environmentVariables = mapOf("CI" to "true")) {
                assertBuildLogDoesNotContain(REQUIRES_INCREMENTAL_COMPILATION_WARNING)
                assertFileExists(CRI_SUBTYPES_TABLE)
            }
        }
    }

    @MavenTest
    @DisplayName("Does not generate CRI when explicitly disabled via a command-line property")
    fun testCriExplicitlyDisabledViaCommandLine(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            setMavenProperty("kotlin.compiler.incremental", "true")

            build("compile", "-Dkotlin.compiler.generateCompilerRefIndex=false") {
                assertBuildLogContains("Using experimental Kotlin incremental compilation")
                assertFileNotExists(CRI_DIR)
            }
        }
    }

    @MavenTest
    @DisplayName("Does not generate CRI when explicitly disabled via the plugin configuration")
    fun testCriExplicitlyDisabledViaPluginConfiguration(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            addKotlinPluginLevelConfiguration("<generateCompilerRefIndex>false</generateCompilerRefIndex>")
            setMavenProperty("kotlin.compiler.incremental", "true")

            build("compile") {
                assertBuildLogContains("Using experimental Kotlin incremental compilation")
                assertFileNotExists(CRI_DIR)
            }
        }
    }

    private companion object {
        const val CRI_DIR = "target/kotlin-ic/compile/cri"
        const val CRI_SUBTYPES_TABLE = "$CRI_DIR/subtypes.table"
        const val REQUIRES_INCREMENTAL_COMPILATION_WARNING = "Compiler reference index generation requires incremental compilation"
    }
}

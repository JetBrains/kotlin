/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInBuildToolsApi
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName

@DisplayName("Kotlin compiler version resolution via the Build Tools API")
@JvmGradlePluginTests
@MustRunOnChangesInGradle
@MustRunOnChangesInBuildToolsApi
class BuildToolsApiCompilerVersionIT : KGPBaseTest() {
    @GradleTest
    @DisplayName("Non-existent compiler version reports KotlinCompilerVersionNotFound")
    fun compileWithNonExistentCompilerVersion(gradleVersion: GradleVersion) =
        testNonExistentCompilerVersion(gradleVersion, defaultBuildOptions)

    @GradleTest
    @DisplayName("Non-existent compiler version reports KotlinCompilerVersionNotFound without configuration cache")
    fun compileWithNonExistentCompilerVersionWithoutConfigurationCache(gradleVersion: GradleVersion) =
        testNonExistentCompilerVersion(
            gradleVersion,
            defaultBuildOptions.copy(
                configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED,
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
            ),
        )

    private fun testNonExistentCompilerVersion(gradleVersion: GradleVersion, buildOptions: BuildOptions) {
        project("simpleProject", gradleVersion, buildOptions = buildOptions, compilerVersion = NON_EXISTENT_COMPILER) {
            buildAndFail("compileKotlin") {
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.KotlinCompilerVersionNotFound,
                    withSubstring = "Could not find Kotlin compiler version '$NON_EXISTENT_COMPILER' set via 'kotlin.compilerVersion'",
                )
                // Build fails while the task graph is calculated, before :compileKotlin is scheduled.
                assertTasksAreNotInTaskGraph(":compileKotlin")
            }
        }
    }

    @GradleTest
    @DisplayName("Non-existent compiler version from a resolution rule reports KotlinCompilerVersionNotFound")
    fun compileWithNonExistentCompilerVersionFromResolutionRule(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion, compilerVersion = TestVersions.Kotlin.STABLE_RELEASE) {
            forceNonExistentCompilerVersion()
            buildAndFail("compileKotlin") {
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.KotlinCompilerVersionNotFound,
                    withSubstring = "A dependency resolution rule replaced version '${TestVersions.Kotlin.STABLE_RELEASE}'",
                )
                // Build fails while the task graph is calculated, before :compileKotlin is scheduled.
                assertTasksAreNotInTaskGraph(":compileKotlin")
            }
        }
    }

    @GradleTest
    @DisplayName("Switching between non-existent and existing compiler versions reports KotlinCompilerVersionNotFound only for the missing one")
    fun compileAfterSwitchingCompilerVersion(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            chooseCompilerVersion(NON_EXISTENT_COMPILER)
            buildAndFail("compileKotlin") {
                assertHasDiagnostic(KotlinToolingDiagnostics.KotlinCompilerVersionNotFound)
            }

            chooseCompilerVersion(TestVersions.Kotlin.STABLE_RELEASE)
            build("compileKotlin") {
                assertNoDiagnostic(KotlinToolingDiagnostics.KotlinCompilerVersionNotFound)
            }

            chooseCompilerVersion(NON_EXISTENT_COMPILER)
            buildAndFail("compileKotlin") {
                assertHasDiagnostic(KotlinToolingDiagnostics.KotlinCompilerVersionNotFound)
            }
        }
    }

    @GradleTest
    @DisplayName("KT-72247: API version unsupported by the default compiler can be used with an older compiler version")
    fun unsupportedApiVersionWithOlderCompilerVersion(gradleVersion: GradleVersion) {
        project("sourceSetsKotlinDsl", gradleVersion, compilerVersion = COMPILER_SUPPORTING_1_8) {
            setUnsupportedApiVersion()
            build("compileKotlin")
        }
    }

    @GradleTest
    @GradleTestVersions(maxVersion = TestVersions.Gradle.G_8_14) // KT-91195: Gradle 9+ does not report deprecated enum entries
    @DisplayName("KT-72247: API version unsupported by the default compiler is reported as deprecated")
    fun unsupportedApiVersionDeprecation(gradleVersion: GradleVersion) {
        project("sourceSetsKotlinDsl", gradleVersion) {
            gradleProperties.append("org.gradle.kotlin.dsl.allWarningsAsErrors=true")
            setUnsupportedApiVersion()
            buildAndFail("help") {
                assertOutputContains("'KOTLIN_1_8' is deprecated. Unsupported by the default compiler")
                assertOutputContains("Requires a 'kotlin.compilerVersion' that supports it.")
            }
        }
    }

    private fun TestProject.setUnsupportedApiVersion() {
        buildGradleKts.append("kotlin.compilerOptions.apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_8)")
    }

    private fun TestProject.forceNonExistentCompilerVersion() {
        buildGradle.append(
            // language=Gradle
            """
            configurations.kotlinBuildToolsApiClasspath.resolutionStrategy.eachDependency {
                if (requested.name == "kotlin-build-tools-impl") useVersion("$NON_EXISTENT_COMPILER")
            }
            """.trimIndent()
        )
    }
}

private const val NON_EXISTENT_COMPILER = "Version.Does.Not.Exist"
private const val COMPILER_SUPPORTING_1_8 = "2.2.21"

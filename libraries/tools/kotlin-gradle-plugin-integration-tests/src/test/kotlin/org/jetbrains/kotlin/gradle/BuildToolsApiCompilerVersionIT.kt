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
    @DisplayName("Non-existent compiler version from a resolution rule without a pinned version reports KotlinCompilerVersionNotFound")
    fun compileWithNonExistentCompilerVersionFromResolutionRuleWithoutPinnedVersion(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            forceNonExistentCompilerVersion()
            buildAndFail("compileKotlin") {
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.KotlinCompilerVersionNotFound,
                    withSubstring = "A dependency resolution rule replaced version '${TestVersions.Kotlin.CURRENT}', the Kotlin Gradle plugin version.",
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

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyJvm
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInBuildToolsApi
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName

@DisplayName("Custom Kotlin compiler version in projects with the 'kotlin-dsl' plugin")
@JvmGradlePluginTests
@MustRunOnChangesInGradle
@MustRunOnChangesInBuildToolsApi
class BuildToolsApiCompilerVersionWithKotlinDslIT : KGPBaseTest() {
    @GradleTest
    @DisplayName("Custom compiler version with 'kotlin-dsl' reports KotlinCompilerVersionWithKotlinDslPlugin")
    fun customCompilerVersionWithKotlinDsl(gradleVersion: GradleVersion) {
        kotlinDslProject(gradleVersion, compilerVersion = TestVersions.Kotlin.STABLE_RELEASE).build("help") {
            assertHasDiagnostic(
                KotlinToolingDiagnostics.KotlinCompilerVersionWithKotlinDslPlugin,
                withSubstring = "Kotlin compiler version '${TestVersions.Kotlin.STABLE_RELEASE}' set via 'kotlin.compilerVersion'",
            )
        }
    }

    @GradleTest
    @DisplayName("Default compiler version with 'kotlin-dsl' does not report KotlinCompilerVersionWithKotlinDslPlugin")
    fun defaultCompilerVersionWithKotlinDsl(gradleVersion: GradleVersion) {
        kotlinDslProject(gradleVersion, compilerVersion = null).build("help") {
            assertNoDiagnostic(KotlinToolingDiagnostics.KotlinCompilerVersionWithKotlinDslPlugin)
        }
    }

    @GradleTest
    @DisplayName("Custom compiler version without 'kotlin-dsl' does not report KotlinCompilerVersionWithKotlinDslPlugin")
    fun customCompilerVersionWithoutKotlinDsl(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion, compilerVersion = TestVersions.Kotlin.STABLE_RELEASE) {
            build("help") {
                assertNoDiagnostic(KotlinToolingDiagnostics.KotlinCompilerVersionWithKotlinDslPlugin)
            }
        }
    }

    private fun kotlinDslProject(gradleVersion: GradleVersion, compilerVersion: String?) = project("emptyKts", gradleVersion) {
        plugins {
            kotlin("jvm")
            id("kotlin-dsl")
        }
        if (compilerVersion != null) {
            buildScriptInjection {
                project.applyJvm {
                    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
                    this.compilerVersion.set(compilerVersion)
                }
            }
        }
    }
}

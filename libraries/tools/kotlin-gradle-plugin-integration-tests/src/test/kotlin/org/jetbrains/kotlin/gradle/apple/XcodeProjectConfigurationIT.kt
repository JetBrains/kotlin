/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for Xcode project configuration checker")
@NativeGradlePluginTests
class XcodeProjectConfigurationIT : KGPBaseTest() {

    @DisplayName("Checker fails when .xcodeproj has no application targets")
    @GradleTest
    fun testXcodeprojHasNoTargets(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            embedDirectoryFromTestData("xcodeProjectConfiguration/iosApp_empty", "iosApp")
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    macosArm64()
                    tvosArm64()
                    watchosArm64()
                }
            }
            buildAndFail(":build") {
                assertHasDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
            }
        }
    }

    @DisplayName("Checker passes when all Kotlin targets are in .xcodeproj")
    @GradleTest
    fun testTargetsMatchesXcodeprojTargets(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            embedDirectoryFromTestData("xcodeProjectConfiguration/iosApp_ios_mac_watch_tv", "iosApp")
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    macosArm64()
                    tvosArm64()
                    watchosArm64()
                }
            }
            build(":tasks") { // Using :tasks is faster than :build for a configuration-only check
                assertNoDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
            }
        }
    }

    @DisplayName("Checker fails when some Kotlin targets are missing from .xcodeproj")
    @GradleTest
    fun testPartialMismatch(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            embedDirectoryFromTestData("xcodeProjectConfiguration/iosApp_ios_mac", "iosApp")
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    macosArm64()
                    tvosArm64()
                    watchosArm64()
                }
            }
            buildAndFail(":build") {
                assertHasDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertOutputContains("'tvosArm64' (expected SDK: 'appletvos')")
                assertOutputContains("'watchosArm64' (expected SDK: 'watchos')")
                assertOutputDoesNotContain("'iosArm64'")
                assertOutputDoesNotContain("'macosArm64'")
            }
        }
    }

    @DisplayName("Checker fails for simulator-only mismatch")
    @GradleTest
    fun testSimulatorVsDeviceMismatch(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            embedDirectoryFromTestData("xcodeProjectConfiguration/iosApp_ios", "iosApp")
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64() // device
                    iosSimulatorArm64() // simulator
                }
            }
            buildAndFail(":build") {
                assertHasDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertOutputContains("'iosSimulatorArm64' (expected SDK: 'iphonesimulator')")
                assertOutputDoesNotContain("'iosArm64'")
            }
        }
    }

    @DisplayName("Checker is skipped when no Apple targets are defined")
    @GradleTest
    fun testNoKotlinAppleTargets(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            embedDirectoryFromTestData("xcodeProjectConfiguration/iosApp_ios_mac_watch_tv", "iosApp")
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                }
            }
            build(":tasks") {
                assertNoDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
            }
        }
    }

    @DisplayName("Checker is skipped when .xcodeproj directory is missing")
    @GradleTest
    fun testXcodeProjectNotFound(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            // No embedDirectoryFromTestData call
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                }
            }
            build(":tasks") {
                assertNoDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
            }
        }
    }

    @DisplayName("Checker handles malformed .pbxproj file gracefully")
    @GradleTest
    fun testMalformedPbxprojFile(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            embedDirectoryFromTestData("xcodeProjectConfiguration/iosApp_malformed", "iosApp")
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                }
            }
            build(":tasks") {
                assertOutputContains("Failed to execute 'plutil' on")
                assertNoDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
            }
        }
    }
}

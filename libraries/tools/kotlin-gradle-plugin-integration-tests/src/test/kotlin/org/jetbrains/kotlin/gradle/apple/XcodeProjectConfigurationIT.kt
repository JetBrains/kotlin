/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeTargetsConfigurationTask.Companion.TASK_NAME
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for Xcode project configuration checker task")
@NativeGradlePluginTests
class XcodeProjectConfigurationIT : KGPBaseTest() {

    private val xcodeCheckTask = ":$TASK_NAME"

    @DisplayName("Checker reports warning when .xcodeproj has no application targets")
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
                }
            }
            build(xcodeCheckTask) {
                assertTasksExecuted(xcodeCheckTask)
                assertHasDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
            }
        }
    }

    @DisplayName("Checker passes when all Kotlin targets are in .xcodeproj")
    @GradleTest
    fun testTargetsMatchXcodeprojTargets(
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
            build(xcodeCheckTask) {
                assertTasksExecuted(xcodeCheckTask)
                assertNoDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
            }
        }
    }

    @DisplayName("Checker reports warning when some Kotlin targets are missing from .xcodeproj")
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
            build(xcodeCheckTask) {
                assertTasksExecuted(xcodeCheckTask)
                assertHasDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
                assertOutputContains("'tvos_arm64' (expected SDK: 'appletvos')")
                assertOutputContains("'watchos_arm64' (expected SDK: 'watchos')")
                assertOutputDoesNotContain("'ios_arm64'")
                assertOutputDoesNotContain("'macos_arm64'")
            }
        }
    }

    @DisplayName("Task is not registered when no Apple targets are defined")
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
                assertTasksAreNotInTaskGraph(xcodeCheckTask)
                assertNoDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
            }
        }
    }

    @DisplayName("Task is skipped when .xcodeproj directory is missing")
    @GradleTest
    fun testXcodeProjectNotFound(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                }
            }
            build(":tasks") {
                assertTasksAreNotInTaskGraph(xcodeCheckTask)
                assertNoDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
            }
        }
    }

    @DisplayName("Task handles malformed xcodeproj gracefully")
    @GradleTest
    fun testMalformedXcodeproj(
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
            build(xcodeCheckTask) {
                assertTasksSkipped(xcodeCheckTask)
                assertNoDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
            }
        }
    }
}

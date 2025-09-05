/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.CheckXcodeTargetsConfigurationTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.ConvertPbxprojToJsonTask
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.appendText

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for Xcode project configuration checker task")
@NativeGradlePluginTests
class CheckXcodeTargetsConfigurationIT : KGPBaseTest() {

    private val convertPbxprojToJsonTask = ":${ConvertPbxprojToJsonTask.TASK_NAME}"
    private val xcodeCheckTask = ":${CheckXcodeTargetsConfigurationTask.TASK_NAME}"
    private val ideaImportTask = ":prepareKotlinIdeaImport"

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
                    iosArm64().binaries.framework()
                }
            }
            build(ideaImportTask) {
                assertTasksExecuted(convertPbxprojToJsonTask, xcodeCheckTask)
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
                    listOf(
                        iosArm64(),
                        macosArm64(),
                        tvosArm64(),
                        watchosArm64(),
                    ).forEach { it.binaries.framework() }
                }
            }
            build(ideaImportTask) {
                assertTasksExecuted(convertPbxprojToJsonTask, xcodeCheckTask)
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
                    listOf(
                        iosArm64(),
                        macosArm64(),
                        tvosArm64(),
                        watchosArm64()
                    ).forEach { it.binaries.framework() }
                }
            }
            build(ideaImportTask) {
                assertTasksExecuted(convertPbxprojToJsonTask, xcodeCheckTask)
                assertHasDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
                assertOutputContains("'tvos_arm64'")
                assertOutputContains("'watchos_arm64'")
                assertOutputDoesNotContain("'ios_arm64'")
                assertOutputDoesNotContain("'macos_arm64'")
            }
        }
    }

    @DisplayName("Tasks are not registered when no Apple targets are defined")
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
            build(ideaImportTask) {
                assertTasksAreNotInTaskGraph(convertPbxprojToJsonTask, xcodeCheckTask)
                assertNoDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
            }
        }
    }

    @DisplayName("Tasks are not registered when .xcodeproj directory is missing")
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
                    iosArm64().binaries.framework()
                }
            }
            build(ideaImportTask) {
                assertTasksAreNotInTaskGraph(convertPbxprojToJsonTask, xcodeCheckTask)
                assertNoDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
            }
        }
    }

    @DisplayName("Tasks handle malformed xcodeproj gracefully")
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
                    iosArm64().binaries.framework()
                }
            }
            build(ideaImportTask) {
                assertTasksExecuted(convertPbxprojToJsonTask, xcodeCheckTask)
                assertOutputContains("Failed to execute 'plutil'")
                assertNoDiagnostic(KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic)
                assertNoDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic)
            }
        }
    }

    @DisplayName("Tasks are UP-TO-DATE on second run and re-execute on change")
    @GradleTest
    fun testTaskIsUpToDateAndReexecutesOnChange(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            embedDirectoryFromTestData("xcodeProjectConfiguration/iosApp_ios", "iosApp")
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64().binaries.framework()
                }
            }

            // First run, should execute both tasks
            build(ideaImportTask) {
                assertTasksExecuted(convertPbxprojToJsonTask, xcodeCheckTask)
            }

            // Second run, without changes, convert task should be up-to-date, check task should re-execute
            build(ideaImportTask) {
                assertTasksUpToDate(convertPbxprojToJsonTask)
                assertTasksExecuted(xcodeCheckTask)
            }

            // Modify the project file
            val pbxproj = projectPath.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")
            pbxproj.appendText("\n// A change")

            // Third run, after change, both tasks should execute again
            build(ideaImportTask) {
                assertTasksExecuted(convertPbxprojToJsonTask, xcodeCheckTask)
            }
        }
    }
}

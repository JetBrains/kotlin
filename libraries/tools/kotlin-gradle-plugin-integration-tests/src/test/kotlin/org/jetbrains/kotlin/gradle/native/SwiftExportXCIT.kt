/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for running Swift Export XCTests")
@SwiftExportGradlePluginTests
@OptIn(ExperimentalSwiftExportDsl::class)
class SwiftExportXCIT : KGPBaseTest() {

    @DisplayName("run XCTests for testing Swift Export")
    @GradleTest
    fun testSwiftExportXCTests(
        gradleVersion: GradleVersion,
    ) {
        XCTestHelpers().use {
            val simulator = it.createSimulator().apply {
                boot()
            }

            project(
                "empty",
                gradleVersion
            ) {
                embedDirectoryFromTestData("simpleSwiftExport")
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    with(project) {
                        applyMultiplatform {
                            iosSimulatorArm64()
                            iosArm64()
                            with(swiftExport) {
                                moduleName.set("Shared")
                                flattenPackage.set("com.github.jetbrains.example")

                                export(project(":subproject")) {
                                    moduleName.set("Subproject")
                                    flattenPackage.set("com.github.jetbrains.library")
                                }
                            }

                            sourceSets.commonMain {
                                compileSource(
                                    """
                                    package com.github.jetbrains.example

                                    fun bar(): Int = 123
                                    fun foo(): Int = 321
                                    fun foobar(param: Int): Int = foo() + bar() + param
                                """.trimIndent()
                                )

                                dependencies {
                                    implementation(project(":subproject"))
                                }
                            }
                        }
                    }
                }

                val subproject = project("emptyKts", gradleVersion) {
                    buildScriptInjection {
                        project.applyMultiplatform {
                            iosSimulatorArm64()
                            iosArm64()
                            sourceSets.commonMain.get().compileSource(
                                """
                                    package com.github.jetbrains.library
                                    
                                    fun libraryFoo(): Int = 123456
                                """.trimIndent()
                            )
                        }
                    }
                }

                include(subproject, "subproject")

                buildXcodeProject(
                    xcodeproj = projectPath.resolve("simpleSwiftExport/iosApp.xcodeproj"),
                    destination = "platform=iOS Simulator,id=${simulator.udid}",
                    action = XcodeBuildAction.Test
                )
            }
        }
    }
}

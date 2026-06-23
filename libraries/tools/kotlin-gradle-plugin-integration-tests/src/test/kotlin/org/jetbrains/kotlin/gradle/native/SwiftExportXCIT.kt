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

    @DisplayName("run XCTests for a user-defined cinterop re-exported by Swift Export")
    @GradleTest
    fun testSwiftExportReexportsUserDefinedCinterop(
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
                // The fixture carries the consuming iosApp project and a user-defined Objective-C module
                // `FooKit` (header + implementation + module map) that the build's cinterop wraps and Swift
                // Export re-exports. The Xcode project finds `FooKit` via SWIFT_INCLUDE_PATHS and links its
                // implementation by compiling FooKit/Foo.m into the app target.
                embedDirectoryFromTestData("cinteropSwiftExport")
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosSimulatorArm64 {
                            compilations.getByName("main").cinterops.create("fookit") { interop ->
                                interop.definitionFile.set(
                                    project.layout.projectDirectory.file("cinteropSwiftExport/cinterop/fookit.def")
                                )
                                interop.includeDirs(project.file("cinteropSwiftExport/FooKit"))
                            }
                        }

                        with(swiftExport) {
                            moduleName.set("Shared")
                            flattenPackage.set("com.github.jetbrains.example")
                            // The Objective-C module name is derived from the `modules` property of the def file.
                            reexportCinterop("fookit", "FooKit")
                        }

                        sourceSets.getByName("iosSimulatorArm64Main").compileSource(
                            """
                            @file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

                            package com.github.jetbrains.example

                            import fookit.Foo

                            fun magicPlusOne(foo: Foo): Int = foo.magic() + 1
                            """.trimIndent()
                        )
                    }
                }

                buildXcodeProject(
                    xcodeproj = projectPath.resolve("cinteropSwiftExport/iosApp.xcodeproj"),
                    destination = "platform=iOS Simulator,id=${simulator.udid}",
                    action = XcodeBuildAction.Test
                )
            }
        }
    }
}

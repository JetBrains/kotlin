/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.createDirectories

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class SwiftPMImportKotlinNativeTestTask : KGPBaseTest() {

    @GradleTest
    fun test(version: GradleVersion) {
        project("empty", version) {
            val dynamicProduct = projectPath.resolve("DynamicProduct").also { it.createDirectories() }.toFile()
            runProcess(listOf("swift", "package", "init", "--type", "library"), dynamicProduct)
            dynamicProduct.resolve("Package.swift").writeText(
                """
                    // swift-tools-version: 5.9
                    import PackageDescription

                    let package = Package(
                        name: "DynamicProduct",
                        products: [
                            .library(
                                name: "DynamicProduct",
                                type: .dynamic,
                                targets: ["DynamicProduct"]
                            ),
                        ],
                        targets: [
                            .target(
                                name: "DynamicProduct"
                            ),
                        ]
                    )
                """.trimIndent()
            )
            dynamicProduct.resolve("Sources/DynamicProduct/DynamicProduct.swift").writeText(
                """
                    import Foundation
                    @objc public class LocalHelper: NSObject {
                        @objc public static func greeting() -> String {
                            return "Hello from dynamic product"
                        }
                    }
                """.trimIndent()
            )

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    iosSimulatorArm64()

                    sourceSets.iosSimulatorArm64Test.get().compileSource(
                        """
                        import kotlin.test.*
                        import swiftPMImport.empty.LocalHelper

                        class DynamicProductTest {
                            @Test
                            fun test() {
                                @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                                println(LocalHelper.greeting())
                            }
                        }

                        """.trimIndent()
                    )

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir("DynamicProduct"),
                            products = listOf("DynamicProduct")
                        )
                    }
                }
            }

            build(":iosSimulatorArm64Test") {
                assertOutputContains("Hello from dynamic product")
            }
        }
    }

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `KT-85114 - incremental linkage smoke test`(version: GradleVersion) {
        project("empty", version) {
            val packageDependency = projectPath.resolve("PackageDependency").also { it.createDirectories() }.toFile()
            fun writeTestSource(version: String) = packageDependency.resolve("Sources/PackageDependency/PackageDependency.swift").writeText(
                """
                    import Foundation
                    @objc public class LocalHelper: NSObject {
                        @objc public static func greeting() -> String {
                            return "FromSwift: ${version}"
                        }
                    }
                """.trimIndent()
            )

            runProcess(listOf("swift", "package", "init", "--type", "library"), packageDependency)
            writeTestSource("1")

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    iosSimulatorArm64()

                    sourceSets.iosSimulatorArm64Test.get().compileSource(
                        """
                        import kotlin.test.*
                        import swiftPMImport.empty.LocalHelper

                        class Tests {
                            @Test
                            fun test() {
                                @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                                println(LocalHelper.greeting())
                            }
                        }

                        """.trimIndent()
                    )

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir("PackageDependency"),
                            products = listOf("PackageDependency")
                        )
                    }
                }
            }

            build(":iosSimulatorArm64Test") {
                assertOutputContains("FromSwift: 1")
            }

            build(":iosSimulatorArm64Test") {
                assertTasksUpToDate(":iosSimulatorArm64Test")
            }

            writeTestSource("2")

            build(":iosSimulatorArm64Test") {
                assertOutputContains("FromSwift: 2")
            }
        }
    }

}
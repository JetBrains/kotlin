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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@GradleTestVersions(
    minVersion = TestVersions.Gradle.G_8_0
)
@DisplayName("SwiftPM import integration tests for explicit Clang module settings")
@SwiftPMImportGradlePluginTests
class SwiftPMImportExplicitClangModulesIT : KGPBaseTest() {

    @GradleTest
    fun `discoverClangModulesImplicitly false with explicit importedClangModules exposes only the listed module`(
        version: GradleVersion,
    ) {
        project("emptyxcode", version) {
            val localPackageRelativePath = "../localMultiModulePackage"
            val localPackageDir = projectPath.resolve(localPackageRelativePath)
            createMultiModuleObjcSwiftPackage(localPackageDir)

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()

                    swiftPMDependencies {
                        discoverClangModulesImplicitly.set(false)
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localPackageRelativePath),
                            products = listOf("ExposedTarget", "HiddenTarget"),
                            importedClangModules = listOf("ExposedTarget"),
                        )
                    }
                }
            }

            assertEquals(
                """
                    swiftPMImport.emptyxcode/ExposedHelper.<init>|objc:init#Constructor[1]
                    swiftPMImport.emptyxcode/ExposedHelper.Companion|null[1]
                    swiftPMImport.emptyxcode/ExposedHelper.init|objc:init[1]
                    swiftPMImport.emptyxcode/ExposedHelperMeta.<init>|<init>(){}[1]
                    swiftPMImport.emptyxcode/ExposedHelperMeta.allocWithZone|objc:allocWithZone:[1]
                    swiftPMImport.emptyxcode/ExposedHelperMeta.alloc|objc:alloc[1]
                    swiftPMImport.emptyxcode/ExposedHelperMeta.greeting|objc:greeting[1]
                    swiftPMImport.emptyxcode/ExposedHelperMeta.new|objc:new[1]
                    swiftPMImport.emptyxcode/ExposedHelperMeta|null[1]
                    swiftPMImport.emptyxcode/ExposedHelper|null[1]
                """.trimIndent(),
                commonizeAndDumpCinteropSignatures().filterOutNoiseSignatures(),
                message = "With implicit discovery disabled and only ExposedTarget listed in importedClangModules, " +
                        "only ExposedHelper symbols should be exposed via cinterop"
            )
        }
    }

    @GradleTest
    fun `discoverClangModulesImplicitly false with default importedClangModules falls back to products`(
        version: GradleVersion,
    ) {
        project("emptyxcode", version) {
            val localPackageRelativePath = "../localMultiModulePackage"
            val localPackageDir = projectPath.resolve(localPackageRelativePath)
            createMultiModuleObjcSwiftPackage(localPackageDir)

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()

                    swiftPMDependencies {
                        discoverClangModulesImplicitly.set(false)
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localPackageRelativePath),
                            products = listOf("ExposedTarget", "HiddenTarget"),
                        )
                    }
                }
            }

            assertEquals(
                """
                    swiftPMImport.emptyxcode/ExposedHelper.<init>|objc:init#Constructor[1]
                    swiftPMImport.emptyxcode/ExposedHelper.Companion|null[1]
                    swiftPMImport.emptyxcode/ExposedHelper.init|objc:init[1]
                    swiftPMImport.emptyxcode/ExposedHelperMeta.<init>|<init>(){}[1]
                    swiftPMImport.emptyxcode/ExposedHelperMeta.allocWithZone|objc:allocWithZone:[1]
                    swiftPMImport.emptyxcode/ExposedHelperMeta.alloc|objc:alloc[1]
                    swiftPMImport.emptyxcode/ExposedHelperMeta.greeting|objc:greeting[1]
                    swiftPMImport.emptyxcode/ExposedHelperMeta.new|objc:new[1]
                    swiftPMImport.emptyxcode/ExposedHelperMeta|null[1]
                    swiftPMImport.emptyxcode/ExposedHelper|null[1]
                    swiftPMImport.emptyxcode/HiddenHelper.<init>|objc:init#Constructor[1]
                    swiftPMImport.emptyxcode/HiddenHelper.Companion|null[1]
                    swiftPMImport.emptyxcode/HiddenHelper.init|objc:init[1]
                    swiftPMImport.emptyxcode/HiddenHelperMeta.<init>|<init>(){}[1]
                    swiftPMImport.emptyxcode/HiddenHelperMeta.allocWithZone|objc:allocWithZone:[1]
                    swiftPMImport.emptyxcode/HiddenHelperMeta.alloc|objc:alloc[1]
                    swiftPMImport.emptyxcode/HiddenHelperMeta.greeting|objc:greeting[1]
                    swiftPMImport.emptyxcode/HiddenHelperMeta.new|objc:new[1]
                    swiftPMImport.emptyxcode/HiddenHelperMeta|null[1]
                    swiftPMImport.emptyxcode/HiddenHelper|null[1]
                """.trimIndent(),
                commonizeAndDumpCinteropSignatures().filterOutNoiseSignatures(),
                message = "With implicit discovery disabled and importedClangModules omitted, the default " +
                        "(products list) should be used and both ExposedHelper and HiddenHelper should be exposed"
            )
        }
    }

    @GradleTest
    fun `clang module without product fail build`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localPackageRelativePath = "../localMultiModulePackage"
            val localPackageDir = projectPath.resolve(localPackageRelativePath)
            createMultiModuleObjcSwiftPackage(localPackageDir)

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()

                    swiftPMDependencies {
                        discoverClangModulesImplicitly.set(false)
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localPackageRelativePath),
                            products = listOf("ExposedTarget"),
                            importedClangModules = listOf("ExposedTarget", "NonProductTarget")
                        )
                    }
                }
            }


            buildAndFail("commonizeCInterop") {
                assertOutputContains("fatal error: module 'NonProductTarget' not found")
            }
        }
    }
}

private fun createMultiModuleObjcSwiftPackage(localPackageDir: Path) {
    localPackageDir.createDirectories()
    localPackageDir.resolve("Package.swift").writeText(
        """
            // swift-tools-version: 5.9
            import PackageDescription

            let package = Package(
                name: "MultiModulePackage",
                platforms: [.iOS(.v15)],
                products: [
                    .library(name: "ExposedTarget", targets: ["ExposedTarget", "AnotherExposedTarget"]),
                    .library(name: "HiddenTarget", targets: ["HiddenTarget"]),
                ],
                targets: [
                    .target(name: "ExposedTarget"),
                    .target(name: "AnotherExposedTarget"),
                    .target(name: "HiddenTarget"),
                    .target(name: "NonProductTarget"),
                ]
            )
        """.trimIndent()
    )

    writeObjcTargetSources(
        targetDir = localPackageDir.resolve("Sources/ExposedTarget"),
        targetName = "ExposedTarget",
        className = "ExposedHelper",
    )
    writeObjcTargetSources(
        targetDir = localPackageDir.resolve("Sources/AnotherExposedTarget"),
        targetName = "AnotherExposedTarget",
        className = "AnotherExposedHelper",
    )
    writeObjcTargetSources(
        targetDir = localPackageDir.resolve("Sources/HiddenTarget"),
        targetName = "HiddenTarget",
        className = "HiddenHelper",
    )
    writeObjcTargetSources(
        targetDir = localPackageDir.resolve("Sources/NonProductTarget"),
        targetName = "NonProductTarget",
        className = "NonProductHelper",
    )
}

private fun writeObjcTargetSources(targetDir: Path, targetName: String, className: String) {
    val includeDir = targetDir.resolve("include").createDirectories()
    includeDir.resolve("$targetName.h").writeText(
        """
            #import <Foundation/Foundation.h>

            @interface $className : NSObject
            + (NSString *)greeting;
            @end
        """.trimIndent()
    )
    targetDir.resolve("$targetName.m").writeText(
        """
            #import "$targetName.h"

            @implementation $className
            + (NSString *)greeting {
                return @"Hello from $className";
            }
            @end
        """.trimIndent()
    )
}

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
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@GradleTestVersions(
    minVersion = TestVersions.Gradle.G_8_0
)
@OptIn(EnvironmentalVariablesOverride::class)
@DisplayName("SwiftPM import integration tests for local packages")
@SwiftPMImportGradlePluginTests
class SwiftPMImportLocalPackagesIT : KGPBaseTest() {

    @GradleTest
    fun `local package cinterop klib signatures are updated when Swift source changes`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localSwiftPackageRelativePath = "../localSwiftPackage"
            val localPackageDir = projectPath.resolve(localSwiftPackageRelativePath)
            val targetName = "LocalSwiftPackage"

            createLocalSwiftPackage(localPackageDir, packageName = targetName)

            // Overwrite the default Swift source with custom content for this test
            localPackageDir.resolve("Sources/$targetName/$targetName.swift").writeText(
                """
                    import Foundation

                    @objc public class OriginalClass: NSObject {
                        @objc public func originalMethod() -> String {
                            return "original"
                        }
                        @objc public func methodToBeRemoved() -> String {
                            return "will be removed"
                        }
                    }
                """.trimIndent()
            )

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localSwiftPackageRelativePath),
                            products = listOf(targetName),
                        )
                    }
                }
            }

            assertEquals(
                """
                    swiftPMImport.emptyxcode/OriginalClass.<init>|objc:init#Constructor[1]
                    swiftPMImport.emptyxcode/OriginalClass.Companion|null[1]
                    swiftPMImport.emptyxcode/OriginalClass.init|objc:init[1]
                    swiftPMImport.emptyxcode/OriginalClass.methodToBeRemoved|objc:methodToBeRemoved[1]
                    swiftPMImport.emptyxcode/OriginalClass.originalMethod|objc:originalMethod[1]
                    swiftPMImport.emptyxcode/OriginalClassMeta.<init>|<init>(){}[1]
                    swiftPMImport.emptyxcode/OriginalClassMeta.allocWithZone|objc:allocWithZone:[1]
                    swiftPMImport.emptyxcode/OriginalClassMeta.alloc|objc:alloc[1]
                    swiftPMImport.emptyxcode/OriginalClassMeta.new|objc:new[1]
                    swiftPMImport.emptyxcode/OriginalClassMeta|null[1]
                    swiftPMImport.emptyxcode/OriginalClass|null[1]
                """.trimIndent(),
                commonizeAndDumpCinteropSignatures().filterOutNoiseSignatures(),
                message = "Initial cinterop signatures should match expected output"
            )

            localPackageDir.resolve("Sources/$targetName/$targetName.swift").writeText(
                """
                    import Foundation

                    @objc public class OriginalClass: NSObject {
                        @objc public func originalMethod() -> String {
                            return "original"
                        }
                    }

                    @objc public class AddedClass: NSObject {
                        @objc public func addedMethod() -> String {
                            return "added"
                        }
                    }
                """.trimIndent()
            )

            assertEquals(
                """
                    swiftPMImport.emptyxcode/AddedClass.<init>|objc:init#Constructor[1]
                    swiftPMImport.emptyxcode/AddedClass.Companion|null[1]
                    swiftPMImport.emptyxcode/AddedClass.addedMethod|objc:addedMethod[1]
                    swiftPMImport.emptyxcode/AddedClass.init|objc:init[1]
                    swiftPMImport.emptyxcode/AddedClassMeta.<init>|<init>(){}[1]
                    swiftPMImport.emptyxcode/AddedClassMeta.allocWithZone|objc:allocWithZone:[1]
                    swiftPMImport.emptyxcode/AddedClassMeta.alloc|objc:alloc[1]
                    swiftPMImport.emptyxcode/AddedClassMeta.new|objc:new[1]
                    swiftPMImport.emptyxcode/AddedClassMeta|null[1]
                    swiftPMImport.emptyxcode/AddedClass|null[1]
                    swiftPMImport.emptyxcode/OriginalClass.<init>|objc:init#Constructor[1]
                    swiftPMImport.emptyxcode/OriginalClass.Companion|null[1]
                    swiftPMImport.emptyxcode/OriginalClass.init|objc:init[1]
                    swiftPMImport.emptyxcode/OriginalClass.originalMethod|objc:originalMethod[1]
                    swiftPMImport.emptyxcode/OriginalClassMeta.<init>|<init>(){}[1]
                    swiftPMImport.emptyxcode/OriginalClassMeta.allocWithZone|objc:allocWithZone:[1]
                    swiftPMImport.emptyxcode/OriginalClassMeta.alloc|objc:alloc[1]
                    swiftPMImport.emptyxcode/OriginalClassMeta.new|objc:new[1]
                    swiftPMImport.emptyxcode/OriginalClassMeta|null[1]
                    swiftPMImport.emptyxcode/OriginalClass|null[1]
                """.trimIndent(),
                commonizeAndDumpCinteropSignatures().filterOutNoiseSignatures(),
                message = "Updated cinterop signatures should match expected output"
            )
        }
    }

    @GradleTest
    fun `local package with objc sources`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localPackageRelativePath = "../localObjcPackage"
            val localPackageDir = projectPath.resolve(localPackageRelativePath)
            val targetName = "LocalObjcPackage"

            createLocalSwiftPackage(localPackageDir, packageName = targetName, sourceLanguage = SwiftPackageSourceLanguage.OBJC)

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localPackageRelativePath),
                            products = listOf(targetName),
                        )
                    }
                }
            }

            assertEquals(
                """
                    swiftPMImport.emptyxcode/LocalHelper.<init>|objc:init#Constructor[1]
                    swiftPMImport.emptyxcode/LocalHelper.Companion|null[1]
                    swiftPMImport.emptyxcode/LocalHelper.init|objc:init[1]
                    swiftPMImport.emptyxcode/LocalHelperMeta.<init>|<init>(){}[1]
                    swiftPMImport.emptyxcode/LocalHelperMeta.allocWithZone|objc:allocWithZone:[1]
                    swiftPMImport.emptyxcode/LocalHelperMeta.alloc|objc:alloc[1]
                    swiftPMImport.emptyxcode/LocalHelperMeta.greeting|objc:greeting[1]
                    swiftPMImport.emptyxcode/LocalHelperMeta.new|objc:new[1]
                    swiftPMImport.emptyxcode/LocalHelperMeta|null[1]
                    swiftPMImport.emptyxcode/LocalHelper|null[1]
                """.trimIndent(),
                commonizeAndDumpCinteropSignatures().filterOutNoiseSignatures(),
                message = "Cinterop signatures should match expected output for local package with ObjC sources"
            )
        }
    }

    @GradleTest
    fun `check that cpp packages with c compatible header visible in kotlin`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localPackageRelativePath = "../localCxxPackage"
            val localPackageDir = projectPath.resolve(localPackageRelativePath)
            val targetName = "LocalCxxPackage"

            createLocalSwiftPackage(
                localPackageDir,
                packageName = targetName,
                sourceLanguage = SwiftPackageSourceLanguage.CXX_WITH_C_HEADER
            )

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localPackageRelativePath),
                            products = listOf(targetName),
                        )
                    }
                }
            }

            kotlinSourcesDir("iosMain")
                .createDirectories().resolve("temp.kt")
                .createFile()
                .writeText("class IosMain")

            assertEquals(
                "swiftPMImport.emptyxcode/cxx_greeting|cxx_greeting(){}[1]",
                commonizeAndDumpCinteropSignatures().trim(),
                message = "Cinterop signatures should be empty for local package with C++ sources"
            )

            projectPath.resolve("iosApp/iosApp/iOSApp.swift").writeText(
                """
                    import SwiftUI
                    import LocalCxxPackage

                    @main
                    struct iOSApp: App {
                        var body: some Scene {
                            WindowGroup {
                                let _ = String(cString: cxx_greeting())
                            }
                        }
                    }
                """.trimIndent()
            )

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj"
                )
            )

            buildXcodeProject(
                xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
            )
        }
    }

    @GradleTest
    fun `check that cpp packages without c compatible header and without any valid package will fail cinterop`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localPackageRelativePath = "../localCxxPackage"
            val localPackageDir = projectPath.resolve(localPackageRelativePath)
            val targetName = "LocalCxxPackage"

            createLocalSwiftPackage(localPackageDir, packageName = targetName, sourceLanguage = SwiftPackageSourceLanguage.CXX)

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localPackageRelativePath),
                            products = listOf(targetName),
                        )
                    }
                }
            }

            buildAndFail("commonizeCInterop")
        }
    }

    @GradleTest
    fun `check that cpp packages without c compatible header but with valid packages will not be visible`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localPackageRelativePath = "../localCxxPackage"
            val localPackageDir = projectPath.resolve(localPackageRelativePath)
            val targetName = "LocalCxxPackage"

            createLocalSwiftPackage(localPackageDir, packageName = targetName, sourceLanguage = SwiftPackageSourceLanguage.CXX)

            val validLocalPackageRelativePath = "../localObjcPackage"
            val validLocalPackageDir = projectPath.resolve(validLocalPackageRelativePath)
            val validTargetName = "LocalPackage"

            createLocalSwiftPackage(
                validLocalPackageDir,
                packageName = validTargetName,
                sourceLanguage = SwiftPackageSourceLanguage.SWIFT_WITH_OBJC
            )

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localPackageRelativePath),
                            products = listOf(targetName),
                        )

                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(validLocalPackageRelativePath),
                            products = listOf(validTargetName),
                        )
                    }
                }
            }

            kotlinSourcesDir("iosMain")
                .createDirectories().resolve("temp.kt")
                .createFile()
                .writeText("class IosMain")


            build("commonizeCInterop") {
                assertOutputContains("fatal error: 'string' file not found")
            }

            val signatures = commonizeAndDumpCinteropSignatures()
            assertEquals(
                """
                    swiftPMImport.emptyxcode/LocalHelper.<init>|objc:init#Constructor[1]
                    swiftPMImport.emptyxcode/LocalHelper.Companion|null[1]
                    swiftPMImport.emptyxcode/LocalHelper.init|objc:init[1]
                    swiftPMImport.emptyxcode/LocalHelperMeta.<init>|<init>(){}[1]
                    swiftPMImport.emptyxcode/LocalHelperMeta.allocWithZone|objc:allocWithZone:[1]
                    swiftPMImport.emptyxcode/LocalHelperMeta.alloc|objc:alloc[1]
                    swiftPMImport.emptyxcode/LocalHelperMeta.greeting|objc:greeting[1]
                    swiftPMImport.emptyxcode/LocalHelperMeta.new|objc:new[1]
                    swiftPMImport.emptyxcode/LocalHelperMeta|null[1]
                    swiftPMImport.emptyxcode/LocalHelper|null[1]
                """.trimIndent(),
                signatures.filterOutNoiseSignatures(),
                message = "Cinterop signatures should have signatures from the valid package"
            )

            projectPath.resolve("iosApp/iosApp/iOSApp.swift").writeText(
                """
                    import SwiftUI

                    @main
                    struct iOSApp: App {
                        var body: some Scene {
                            WindowGroup { }
                        }
                    }
                """.trimIndent()
            )

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj"
                )
            )

            buildXcodeProject(
                xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
            )
        }
    }

    @GradleTest
    fun `check that swift packages with cxx target do not fail cinterop and built in xcode`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localPackageRelativePath = "../localPackage"
            val localPackageDir = projectPath.resolve(localPackageRelativePath).createDirectories()
            val targetName = "LocalPackage"
            localPackageDir.resolve("Package.swift").writeText(
                """
            // swift-tools-version: 5.9
            import PackageDescription

            let package = Package(
                name: "$targetName",
                platforms: [.iOS(.v15)],
                products: [
                    .library(name: "$targetName", targets: ["$targetName"]),
                ],
                targets: [
                    .target(name: "LocalCxxPackage"),
                    .target(
                        name: "$targetName",
                        dependencies: ["LocalCxxPackage"],
                        swiftSettings: [.interoperabilityMode(.Cxx)]
                    )
                ]
            )
            """.trimIndent()
            )
            writeLocalPackageSources(
                sourcesDir = localPackageDir.resolve("Sources/LocalCxxPackage"),
                packageName = "LocalCxxPackage",
                sourceLanguage = SwiftPackageSourceLanguage.CXX,
            )

            writeLocalPackageSources(
                sourcesDir = localPackageDir.resolve("Sources/$targetName"),
                packageName = targetName,
                sourceLanguage = SwiftPackageSourceLanguage.SWIFT_WITH_OBJC,
            )

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localPackageRelativePath),
                            products = listOf(targetName),
                        )
                    }
                }
            }

            kotlinSourcesDir("iosMain")
                .createDirectories().resolve("temp.kt")
                .createFile()
                .writeText("class IosMain")


            build("commonizeCInterop") {
                assertOutputContains("fatal error: 'string' file not found")
            }

            projectPath.resolve("iosApp/iosApp/iOSApp.swift").writeText(
                """
                    import SwiftUI
                    import LocalCxxPackage

                    @main
                    struct iOSApp: App {
                        var body: some Scene {
                            WindowGroup {
                                let _ = cxx_greeting()
                            }
                        }
                    }
                """.trimIndent()
            )

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj"
                )
            )

            buildXcodeProject(
                xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
                buildSettingOverrides = mapOf(Pair("SWIFT_OBJC_INTEROP_MODE", "objcxx"))
            )
        }
    }

    @GradleTest
    fun `check that pure swift packages not visible in kotlin, but passed to xcodebuild`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localPackageRelativePath = "../localPureSwiftPackage"
            val localPackageDir = projectPath.resolve(localPackageRelativePath)
            val targetName = "LocalPureSwiftPackage"

            createLocalSwiftPackage(localPackageDir, packageName = targetName, sourceLanguage = SwiftPackageSourceLanguage.SWIFT)

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localPackageRelativePath),
                            products = listOf(targetName),
                        )
                    }
                }
            }

            kotlinSourcesDir("iosMain")
                .createDirectories().resolve("temp.kt")
                .createFile()
                .writeText("class IosMain")

            assertEquals(
                "",
                commonizeAndDumpCinteropSignatures().filterOutNoiseSignatures(),
                message = "Cinterop signatures should be empty for local package with pure Swift sources"
            )

            projectPath.resolve("iosApp/iosApp/iOSApp.swift").writeText(
                """
                    import SwiftUI
                    import LocalPureSwiftPackage

                    @main
                    struct iOSApp: App {
                        var body: some Scene {
                            WindowGroup {
                                let _ = PureSwiftHelper.greeting()
                            }
                        }
                    }
                """.trimIndent()
            )

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj"
                )
            )

            buildXcodeProject(
                xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
            )
        }
    }

    @GradleTest
    fun `local package with resources - packaging and runtime lookup in iosApp`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localPackageRelativePath = "../localResourcePackage"
            val localPackageDir = projectPath.resolve(localPackageRelativePath)
            val targetName = "LocalResourcePackage"
            val resourceFileName = "greeting.txt"
            val resourceContent = "Hello from SPM resource"

            createLocalSwiftPackageWithResources(
                localPackageDir = localPackageDir,
                packageName = targetName,
                resourceFileName = resourceFileName,
                resourceContent = resourceContent,
            )

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localPackageRelativePath),
                            products = listOf(targetName),
                        )
                    }
                }
            }

            kotlinSourcesDir("iosMain")
                .createDirectories().resolve("temp.kt")
                .createFile()
                .writeText("class IosMain")

            // Update iosApp Swift source to use the resource accessor at runtime
            projectPath.resolve("iosApp/iosApp/iOSApp.swift").writeText(
                """
                    import SwiftUI
                    import LocalResourcePackage

                    @main
                    struct iOSApp: App {
                        var body: some Scene {
                            WindowGroup {
                                let _ = ResourceAccessor.resourceContent()
                                let _ = ResourceAccessor.resourceBundle()
                            }
                        }
                    }
                """.trimIndent()
            )

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj"
                )
            )

            buildXcodeProject(
                xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
            )

            val appBundle = projectPath.resolve("xcodeDerivedData/Build/Products/Debug-iphonesimulator/emptyxcode.app")
            assertTrue(
                appBundle.exists(),
                "App bundle should exist at ${appBundle.absolutePathString()}, but it does not exist"
            )

            val bundledResource = appBundle.resolve("${targetName}_$targetName.bundle/$resourceFileName")

            assertTrue(
                bundledResource.exists(),
                "Resource file '$resourceFileName' should be packaged in the app bundle, " +
                        "but it does not exist at ${bundledResource.absolutePathString()}"
            )

            assertEquals(
                resourceContent,
                bundledResource.readText(),
                "Resource file content should match the original content"
            )
        }
    }

    @GradleTest
    fun `test apple target without macosArm64 slice in swift package`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localSwiftPackageRelativePath = "../localSwiftPackage"
            val localPackageDir = projectPath.resolve(localSwiftPackageRelativePath)
            val targetName = "LocalSwiftPackage"

            createLocalSwiftPackage(localPackageDir, packageName = targetName)

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64(),
                        macosArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = true
                        }
                    }

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localSwiftPackageRelativePath),
                            products = listOf(targetName),
                        )
                    }
                }
            }

            kotlinSourcesDir("appleMain")
                .createDirectories().resolve("temp.kt")
                .createFile()
                .writeText("class IosMain")

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj"
                )
            )

            build(
                "linkDebugFrameworkMacosArm64",
            )

            buildXcodeProject(
                xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
            )
        }
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.konan.target.Xcode
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
                    public open expect class swiftPMImport/emptyxcode/OriginalClass : platform/darwin/NSObject
                    public /* secondary */ constructor swiftPMImport/emptyxcode/OriginalClass.<init>()
                    public open expect fun swiftPMImport/emptyxcode/OriginalClass.init(): swiftPMImport/emptyxcode/OriginalClass
                    public open expect fun swiftPMImport/emptyxcode/OriginalClass.methodToBeRemoved(): kotlin/String
                    public open expect fun swiftPMImport/emptyxcode/OriginalClass.originalMethod(): kotlin/String
                    public final expect companion object swiftPMImport/emptyxcode/OriginalClass.Companion : swiftPMImport/emptyxcode/OriginalClassMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/OriginalClass>
                    public open expect class swiftPMImport/emptyxcode/OriginalClassMeta : platform/darwin/NSObjectMeta
                    protected /* secondary */ constructor swiftPMImport/emptyxcode/OriginalClassMeta.<init>()
                    public open expect fun swiftPMImport/emptyxcode/OriginalClassMeta.alloc(): swiftPMImport/emptyxcode/OriginalClass?
                    public open expect fun swiftPMImport/emptyxcode/OriginalClassMeta.allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/OriginalClass?
                    public open expect fun swiftPMImport/emptyxcode/OriginalClassMeta.new(): swiftPMImport/emptyxcode/OriginalClass?
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
                    public open expect class swiftPMImport/emptyxcode/AddedClass : platform/darwin/NSObject
                    public /* secondary */ constructor swiftPMImport/emptyxcode/AddedClass.<init>()
                    public open expect fun swiftPMImport/emptyxcode/AddedClass.addedMethod(): kotlin/String
                    public open expect fun swiftPMImport/emptyxcode/AddedClass.init(): swiftPMImport/emptyxcode/AddedClass
                    public final expect companion object swiftPMImport/emptyxcode/AddedClass.Companion : swiftPMImport/emptyxcode/AddedClassMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/AddedClass>
                    public open expect class swiftPMImport/emptyxcode/AddedClassMeta : platform/darwin/NSObjectMeta
                    protected /* secondary */ constructor swiftPMImport/emptyxcode/AddedClassMeta.<init>()
                    public open expect fun swiftPMImport/emptyxcode/AddedClassMeta.alloc(): swiftPMImport/emptyxcode/AddedClass?
                    public open expect fun swiftPMImport/emptyxcode/AddedClassMeta.allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/AddedClass?
                    public open expect fun swiftPMImport/emptyxcode/AddedClassMeta.new(): swiftPMImport/emptyxcode/AddedClass?
                    public open expect class swiftPMImport/emptyxcode/OriginalClass : platform/darwin/NSObject
                    public /* secondary */ constructor swiftPMImport/emptyxcode/OriginalClass.<init>()
                    public open expect fun swiftPMImport/emptyxcode/OriginalClass.init(): swiftPMImport/emptyxcode/OriginalClass
                    public open expect fun swiftPMImport/emptyxcode/OriginalClass.originalMethod(): kotlin/String
                    public final expect companion object swiftPMImport/emptyxcode/OriginalClass.Companion : swiftPMImport/emptyxcode/OriginalClassMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/OriginalClass>
                    public open expect class swiftPMImport/emptyxcode/OriginalClassMeta : platform/darwin/NSObjectMeta
                    protected /* secondary */ constructor swiftPMImport/emptyxcode/OriginalClassMeta.<init>()
                    public open expect fun swiftPMImport/emptyxcode/OriginalClassMeta.alloc(): swiftPMImport/emptyxcode/OriginalClass?
                    public open expect fun swiftPMImport/emptyxcode/OriginalClassMeta.allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/OriginalClass?
                    public open expect fun swiftPMImport/emptyxcode/OriginalClassMeta.new(): swiftPMImport/emptyxcode/OriginalClass?
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
                    public open expect class swiftPMImport/emptyxcode/LocalHelper : platform/darwin/NSObject
                    public /* secondary */ constructor swiftPMImport/emptyxcode/LocalHelper.<init>()
                    public open expect fun swiftPMImport/emptyxcode/LocalHelper.init(): swiftPMImport/emptyxcode/LocalHelper?
                    public final expect companion object swiftPMImport/emptyxcode/LocalHelper.Companion : swiftPMImport/emptyxcode/LocalHelperMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/LocalHelper>
                    public open expect class swiftPMImport/emptyxcode/LocalHelperMeta : platform/darwin/NSObjectMeta
                    protected /* secondary */ constructor swiftPMImport/emptyxcode/LocalHelperMeta.<init>()
                    public open expect fun swiftPMImport/emptyxcode/LocalHelperMeta.alloc(): swiftPMImport/emptyxcode/LocalHelper?
                    public open expect fun swiftPMImport/emptyxcode/LocalHelperMeta.allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/LocalHelper?
                    public open expect fun swiftPMImport/emptyxcode/LocalHelperMeta.greeting(): kotlin/String?
                    public open expect fun swiftPMImport/emptyxcode/LocalHelperMeta.new(): swiftPMImport/emptyxcode/LocalHelper?
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
                "public final expect fun swiftPMImport/emptyxcode/cxx_greeting(): kotlinx/cinterop/CPointer<kotlinx/cinterop/ByteVarOf<kotlin/Byte>>?",
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
                    public open expect class swiftPMImport/emptyxcode/LocalHelper : platform/darwin/NSObject
                    public /* secondary */ constructor swiftPMImport/emptyxcode/LocalHelper.<init>()
                    public open expect fun swiftPMImport/emptyxcode/LocalHelper.init(): swiftPMImport/emptyxcode/LocalHelper
                    public final expect companion object swiftPMImport/emptyxcode/LocalHelper.Companion : swiftPMImport/emptyxcode/LocalHelperMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/LocalHelper>
                    public open expect class swiftPMImport/emptyxcode/LocalHelperMeta : platform/darwin/NSObjectMeta
                    protected /* secondary */ constructor swiftPMImport/emptyxcode/LocalHelperMeta.<init>()
                    public open expect fun swiftPMImport/emptyxcode/LocalHelperMeta.alloc(): swiftPMImport/emptyxcode/LocalHelper?
                    public open expect fun swiftPMImport/emptyxcode/LocalHelperMeta.allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/LocalHelper?
                    public open expect fun swiftPMImport/emptyxcode/LocalHelperMeta.greeting(): kotlin/String
                    public open expect fun swiftPMImport/emptyxcode/LocalHelperMeta.new(): swiftPMImport/emptyxcode/LocalHelper?
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

    @GradleTest
    fun `local swift package with valid slices for all apple targets`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localSwiftPackageRelativePath = "../localSwiftPackage"
            val localPackageDir = projectPath.resolve(localSwiftPackageRelativePath).createDirectories()
            val targetName = "LocalSwiftPackage"

            localPackageDir.resolve("Package.swift").writeText(
                """
                    // swift-tools-version: 5.9
                    import PackageDescription

                    let package = Package(
                        name: "$targetName",
                        products: [
                            .library(name: "$targetName", targets: ["$targetName"]),
                        ],
                        targets: [
                            .target(name: "$targetName"),
                        ]
                    )
                """.trimIndent()
            )
            writeLocalPackageSources(
                sourcesDir = localPackageDir.resolve("Sources/$targetName"),
                packageName = targetName,
                sourceLanguage = SwiftPackageSourceLanguage.SWIFT_WITH_OBJC,
            )

            plugins {
                kotlin("multiplatform")
            }

            val isXcodeLessThan27 = Xcode.findCurrent().version.major < 27
            buildScriptInjection {
                project.applyMultiplatform {
                    val targets = mutableListOf(
                        iosArm64(),
                        iosX64(),
                        iosSimulatorArm64(),
                        macosArm64(),
                        tvosArm64(),
                        tvosSimulatorArm64(),
                        watchosArm64(),
                        watchosSimulatorArm64(),
                        watchosDeviceArm64(),
                    )
                    targets.forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = false
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
                .writeText(
                    """
                        class AppleMain
                        
                        @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                        fun localGreeting(): String {
                            return swiftPMImport.emptyxcode.LocalHelper.greeting()
                        }
                    """.trimIndent()
                )

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj"
                )
            )

            val linkTasks = mutableListOf(
                "linkDebugFrameworkIosArm64",
                "linkDebugFrameworkIosX64",
                "linkDebugFrameworkIosSimulatorArm64",
                "linkDebugFrameworkMacosArm64",
                "linkDebugFrameworkTvosArm64",
                "linkDebugFrameworkTvosSimulatorArm64",
                "linkDebugFrameworkWatchosArm64",
                "linkDebugFrameworkWatchosSimulatorArm64",
                "linkDebugFrameworkWatchosDeviceArm64",
            )
            if (isXcodeLessThan27) {
                linkTasks.add("linkDebugFrameworkWatchosArm32")
            }

            build(*linkTasks.toTypedArray())

            buildXcodeProject(
                xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
            )
        }
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.testbase.EnvironmentalVariables
import org.jetbrains.kotlin.gradle.testbase.EnvironmentalVariablesOverride
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertOutputContains
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.buildXcodeProject
import org.jetbrains.kotlin.gradle.testbase.compileSource
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import kotlin.test.assertEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@GradleTestVersions(
    minVersion = TestVersions.Gradle.G_8_0
)
@OptIn(EnvironmentalVariablesOverride::class)
@SwiftPMImportGradlePluginTests
class SwiftPMImportBinaryTargetsIT : KGPBaseTest() {

    @GradleTest
    fun `local package with binaryTarget static framework xcframework`(version: GradleVersion) {
        testLocalPackageWithBinaryTargetXcframework(
            version,
            buildType = XcframeworkBuildType.FRAMEWORK,
            linkage = XcframeworkLinkage.STATIC,
        )
    }

    @GradleTest
    fun `local package with binaryTarget dynamic framework xcframework`(version: GradleVersion) {
        testLocalPackageWithBinaryTargetXcframework(
            version,
            buildType = XcframeworkBuildType.FRAMEWORK,
            linkage = XcframeworkLinkage.DYNAMIC,
        )
    }

    @GradleTest
    fun `local package with binaryTarget static library xcframework`(version: GradleVersion) {
        testLocalPackageWithBinaryTargetXcframework(
            version,
            buildType = XcframeworkBuildType.LIBRARY,
            linkage = XcframeworkLinkage.STATIC
        )
    }

    @GradleTest
    fun `shared xcframework consumed from both kotlin and swift sides with dynamic framework`(version: GradleVersion) {
        project("emptyxcode", version) {
            // 1. Create SharedLib xcframework wrapped in a local Swift package
            val sharedLibPackageRelativePath = "../sharedLibPackage"
            val sharedLibPackageDir = projectPath.resolve(sharedLibPackageRelativePath)
            val xcframework = buildSwiftXCFramework(
                sharedLibPackageDir,
                "SharedLib",
                linkage = XcframeworkLinkage.DYNAMIC,
                buildType = XcframeworkBuildType.FRAMEWORK,
            )
            createLocalSwiftPackageWithBinaryTarget(
                localPackageDir = sharedLibPackageDir,
                packageName = "SharedLib",
                xcframeworkPath = xcframework
            )

            // 2. Create SwiftConsumer package that depends on SharedLib
            val swiftConsumerRelativePath = "../swiftConsumerPackage"
            val swiftConsumerDir = projectPath.resolve(swiftConsumerRelativePath)
            swiftConsumerDir.createDirectories()
            val consumerSourcesDir = swiftConsumerDir.resolve("Sources/SwiftConsumer")
            consumerSourcesDir.createDirectories()

            swiftConsumerDir.resolve("Package.swift").writeText(
                """
                    // swift-tools-version: 5.9
                    import PackageDescription

                    let package = Package(
                        name: "SwiftConsumer",
                        platforms: [.iOS(.v15)],
                        products: [
                            .library(name: "SwiftConsumer", targets: ["SwiftConsumer"]),
                        ],
                        dependencies: [
                            .package(path: "../sharedLibPackage"),
                        ],
                        targets: [
                            .target(
                                name: "SwiftConsumer",
                                dependencies: [
                                    .product(name: "SharedLib", package: "sharedLibPackage"),
                                ]
                            ),
                        ]
                    )
                """.trimIndent()
            )

            consumerSourcesDir.resolve("SwiftConsumer.swift").writeText(
                """
                    import Foundation
                    import SharedLib

                    @objc public class SwiftConsumerHelper: NSObject {
                        @objc public static func createLocalHelper() -> LocalHelper {
                            return LocalHelper()
                        }
                    }
                """.trimIndent()
            )

            // 3. Configure dynamic KMP framework
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
                            isStatic = false
                        }
                    }

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(sharedLibPackageRelativePath),
                            products = listOf("SharedLib"),
                        )
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(swiftConsumerRelativePath),
                            products = listOf("SwiftConsumer"),
                        )
                    }
                }
            }

            kotlinSourcesDir("iosMain")
                .createDirectories().resolve("temp.kt")
                .createFile()
                .writeText("class IosMain")

            projectPath.resolve("iosApp/iosApp/iOSApp.swift").writeText(
                """
                    import SwiftUI
                    import SharedLib

                    @main
                    struct iOSApp: App {
                        var body: some Scene {
                            WindowGroup {
                                let _ = LocalHelper()
                            }
                        }
                    }
                """.trimIndent()
            )

            // 5. Build and verify no duplicate symbol errors
            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj"
                )
            )

            buildXcodeProject(
                xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
                // The test xcframework only contains arm64 slices
                buildSettingOverrides = mapOf("ARCHS" to "arm64"),
            )
        }
    }

    @GradleTest
    fun `test apple target without macosArm64 slice in swift package with binary target`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localSwiftPackageRelativePath = "../localSwiftPackage"
            val localPackageDir = projectPath.resolve(localSwiftPackageRelativePath)
            val targetName = "LocalSwiftPackage"

            val xcframework = buildSwiftXCFramework(localPackageDir, targetName)
            createLocalSwiftPackageWithBinaryTarget(localPackageDir, packageName = targetName, xcframeworkPath = xcframework)

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
                            products = listOf(product(targetName))
                        )
                    }
                }
            }

            buildAndFail("assemble")
        }
    }

    @GradleTest
    fun `test apple target without macosArm64 slice in swift package with binary target and product filter`(version: GradleVersion) {
        project("emptyxcode", version) {
            val localSwiftPackageRelativePath = "../localSwiftPackage"
            val localPackageDir = projectPath.resolve(localSwiftPackageRelativePath)
            val targetName = "LocalSwiftPackage"

            val xcframework = buildSwiftXCFramework(localPackageDir, targetName)
            createLocalSwiftPackageWithBinaryTarget(localPackageDir, packageName = targetName, xcframeworkPath = xcframework)

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
                            products = listOf(product(targetName, platforms = setOf(SwiftPMDependency.Platform.iOS))),
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

    private fun testLocalPackageWithBinaryTargetXcframework(
        version: GradleVersion,
        linkage: XcframeworkLinkage,
        buildType: XcframeworkBuildType,
    ) {
        project("emptyxcode", version) {
            val frameworkName = "BinaryLib"
            val localPackageRelativePath = "../localBinaryPackage"
            val localPackageDir = projectPath.resolve(localPackageRelativePath)

            val xcframework = buildSwiftXCFramework(
                localPackageDir,
                frameworkName,
                linkage = linkage,
                buildType = buildType,
            )

            createLocalSwiftPackageWithBinaryTarget(
                localPackageDir = localPackageDir,
                packageName = frameworkName,
                xcframeworkPath = xcframework
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
                            products = listOf(frameworkName),
                        )
                    }
                }
            }

            kotlinSourcesDir("iosMain")
                .createDirectories().resolve("temp.kt")
                .createFile()
                .writeText("class IosMain")

            kotlinSourcesDir("iosTest")
                .createDirectories().resolve("test.kt")
                .createFile()
                .writeText(
                    """
                        import kotlin.test.*
                        import swiftPMImport.emptyxcode.LocalHelper

                        class DynamicProductTest {
                            @Test
                            fun test() {
                                @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                                println(LocalHelper.greeting())
                            }
                        }

                        """.trimIndent()
                )

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
                message = "Cinterop signatures should match expected output for linkage=$linkage, buildType=$buildType"
            )

            projectPath.resolve("iosApp/iosApp/iOSApp.swift").writeText(
                """
                    import SwiftUI
                    import BinaryLib

                    @main
                    struct iOSApp: App {
                        var body: some Scene {
                            WindowGroup {
                                let _ = LocalHelper()
                                let _ = LocalHelper.greeting()
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

            build(":iosSimulatorArm64Test") {
                assertOutputContains("Hello from LocalHelper")
            }

            buildXcodeProject(
                xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
            )
        }
    }
}

// targets inside emptyxcode iosApp project
private const val FRAMEWORK_TARGET_NAME = "SwiftFrameworkTarget"
private const val STATIC_LIBRARY_TARGET_NAME = "SwiftStaticLibraryTarget"

private data class AppleSdkSlice(
    val sdk: String,
    val arch: String = "arm64",
    val destination: String,
)

private val iosFrameworkSlices = listOf(
    AppleSdkSlice(sdk = "iphoneos", destination = "generic/platform=iOS"),
    AppleSdkSlice(sdk = "iphonesimulator", destination = "generic/platform=iOS Simulator"),
)

private enum class XcframeworkLinkage {
    STATIC,
    DYNAMIC,
}

/**
 * Library will pack .a (static) binary to the xcframework, a framework will pack .framework binary to the xcframework
 */
private enum class XcframeworkBuildType {
    LIBRARY,
    FRAMEWORK,
}

private fun TestProject.buildSwiftXCFramework(
    outputDir: Path,
    frameworkName: String,
    linkage: XcframeworkLinkage = XcframeworkLinkage.STATIC,
    buildType: XcframeworkBuildType = XcframeworkBuildType.FRAMEWORK,
    workDir: Path = projectPath.resolve("iosApp"),
): Path {
    outputDir.createDirectories()
    return when (buildType) {
        XcframeworkBuildType.FRAMEWORK -> {
            val archives = iosFrameworkSlices.map { slice ->
                buildSwiftFramework(
                    workDir = workDir,
                    frameworkName = frameworkName,
                    slice = slice,
                    linkage = linkage,
                )
            }
            createXCFramework(outputDir, frameworkName, archives = archives)
        }
        XcframeworkBuildType.LIBRARY -> {
            if (linkage == XcframeworkLinkage.DYNAMIC) {
                error("Dynamic library is not supported for Swift static library in test infrastructure")
            }
            val libraries = iosFrameworkSlices.map { slice ->
                buildSwiftStaticLibrary(
                    workDir = workDir,
                    frameworkName = frameworkName,
                    slice = slice,
                )
            }
            createXCFramework(outputDir, frameworkName, libraries = libraries)
        }
    }
}

private fun buildSwiftFramework(
    workDir: Path,
    baseXcodeTarget: String = FRAMEWORK_TARGET_NAME,
    frameworkName: String,
    slice: AppleSdkSlice,
    linkage: XcframeworkLinkage,
): Path {
    workDir.resolve("$baseXcodeTarget/$baseXcodeTarget.swift").writeText(swiftSourceContent())

    val archivePath = workDir.resolve("$frameworkName-${slice.sdk}.xcarchive")
    val command = buildList {
        addAll(
            listOf(
                "xcodebuild", "archive",
                "-project", "iosApp.xcodeproj",
                "-scheme", baseXcodeTarget,
                "-destination", slice.destination,
                "-archivePath", archivePath.absolutePathString(),
                "ARCHS=${slice.arch}",
                "SKIP_INSTALL=NO",
                "PRODUCT_NAME=$frameworkName",
                "SWIFT_INSTALL_OBJC_HEADER=YES",
                "BUILD_LIBRARY_FOR_DISTRIBUTION=YES",
                "DEFINES_MODULE=YES",
                when (linkage) {
                    XcframeworkLinkage.STATIC -> "MACH_O_TYPE=staticlib"
                    XcframeworkLinkage.DYNAMIC -> "MACH_O_TYPE=mh_dylib"
                },
            )
        )
    }
    runProcessOrFail(
        command = command,
        workingDir = workDir,
        errorMessage = "Failed to archive Swift $linkage framework for ${slice.sdk}-${slice.arch}",
    )

    return archivePath
}

private data class LibraryArtifact(
    val frameworkName: String,
    val buildDir: Path,
) {
    val binaryPath: Path
        get() = buildDir.resolve("lib$frameworkName.a")

    val headersDir: Path
        get() = buildDir.resolve("Headers")
}

private fun buildSwiftStaticLibrary(
    workDir: Path,
    baseXcodeTarget: String = STATIC_LIBRARY_TARGET_NAME,
    frameworkName: String,
    slice: AppleSdkSlice,
): LibraryArtifact {
    workDir.resolve("$baseXcodeTarget/$baseXcodeTarget.swift").writeText(swiftSourceContent())

    val library = LibraryArtifact(
        frameworkName = frameworkName,
        buildDir = workDir.resolve("lib$frameworkName-${slice.sdk}-build").createDirectories(),
    )

    val command = listOf(
        "xcodebuild", "build",
        "-project", "iosApp.xcodeproj",
        "-scheme", baseXcodeTarget,
        "-destination", slice.destination,
        "ARCHS=${slice.arch}",
        "CONFIGURATION_BUILD_DIR=${library.buildDir.absolutePathString()}",
        "PRODUCT_NAME=$frameworkName",
        "BUILD_LIBRARY_FOR_DISTRIBUTION=YES",
        "SWIFT_INSTALL_OBJC_HEADER=YES",
        "SWIFT_OBJC_INTERFACE_HEADER_DIR=${library.headersDir.absolutePathString()}",
    )
    runProcessOrFail(
        command = command,
        workingDir = workDir,
        errorMessage = "Failed to build Swift static library for ${slice.sdk}-${slice.arch}",
    )

    library.headersDir.resolve("module.modulemap").writeText(
        """
        module $frameworkName {
            header "$frameworkName-Swift.h"
            export *
        }
        """.trimIndent()
    )

    return library
}

private fun createXCFramework(
    outputDir: Path,
    frameworkName: String,
    archives: List<Path> = emptyList(),
    libraries: List<LibraryArtifact> = emptyList(),
): Path {
    val xcframeworkPath = outputDir.resolve("$frameworkName.xcframework")
    val command = buildList {
        addAll(listOf("xcodebuild", "-create-xcframework"))
        archives.forEach { archive ->
            add("-archive")
            add(archive.absolutePathString())
            add("-framework")
            add("$frameworkName.framework")
        }
        libraries.forEach { library ->
            add("-library")
            add(library.binaryPath.absolutePathString())
            add("-headers")
            add(library.headersDir.absolutePathString())
        }
        add("-output")
        add(xcframeworkPath.absolutePathString())
    }
    runProcessOrFail(
        command = command,
        workingDir = outputDir,
        errorMessage = "Failed to create XCFramework",
    )

    return xcframeworkPath
}

private fun runProcessOrFail(
    command: List<String>,
    workingDir: Path,
    errorMessage: String,
) {
    val result = runProcess(
        cmd = command,
        workingDir = workingDir.toFile(),
    )
    require(result.isSuccessful) {
        "$errorMessage: ${result.output}"
    }
}

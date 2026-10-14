/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.apple.describeSwiftPackage
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.util.ProcessRunResult
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for the Swift package integration of the export DSL")
@SwiftExportGradlePluginTests
@OptIn(ExperimentalSwiftExportDsl::class)
class SwiftPackageExportIT : KGPBaseTest() {

    @DisplayName("A dependency with a different public API per target is exported for every destination")
    @GradleTest
    fun testPerTargetApiOfACommonDependency(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()
                    macosArm64()
                    sourceSets.commonMain {
                        compileSource("fun sharedApi(): Int = commonApi()")
                        dependencies {
                            api(project(":common"))
                        }
                    }
                }
                export.swift {
                    moduleName.set("Shared")
                    swiftPackageIntegration {
                        outputDirectory.set(project.layout.projectDirectory.dir("package"))
                    }
                }
            }

            val common = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        iosSimulatorArm64()
                        macosArm64()
                        sourceSets.commonMain.get().compileSource("fun commonApi(): Int = 1")
                        sourceSets.getByName("iosArm64Main").compileSource("fun iosArm64Api(): Int = 2")
                        sourceSets.getByName("iosSimulatorArm64Main").compileSource("fun iosSimulatorArm64Api(): Int = 3")
                        sourceSets.getByName("macosArm64Main").compileSource("fun macosArm64Api(): Int = 4")
                    }
                }
            }

            include(common, "common")

            build(":exportDebugSwiftPackage") {
                assertTasksExecuted(
                    ":iosArm64SwiftPackageExport",
                    ":iosSimulatorArm64SwiftPackageExport",
                    ":macosArm64SwiftPackageExport",
                    ":generateDebugSwiftPackage",
                    ":assembleDebugSwiftPackageBinary",
                    ":exportDebugSwiftPackage",
                )
            }

            // The headers are only collected for the Xcode integration, so neither package has them.
            assertDirectoryDoesNotExist(projectPath.resolve("build/SwiftPackage/Debug/package/OtherIncludes"))
            val exportedPackage = projectPath.resolve("package/Debug")
            assertDirectoryDoesNotExist(exportedPackage.resolve("OtherIncludes"))
            assertEquals(
                setOf("ios-arm64", "ios-arm64-simulator", "macos-arm64"),
                exportedPackage.resolve("SharedKotlin.xcframework").directoryNames(),
            )

            val manifest = describeSwiftPackage(exportedPackage)
            assertEquals(mapOf("ios" to "18.0", "macos" to "15.0"), manifest.platforms.associate { it.name to it.version })
            assertEquals(listOf("SharedLibrary"), manifest.products.map { it.name })
            val targets = manifest.targets.associateBy { it.name }
            assertEquals("binary", targets.getValue("SharedKotlin").type)
            assertEquals("SharedKotlin.xcframework", targets.getValue("SharedKotlin").path)
            // The Kotlin runtime target is what puts the Kotlin binary on the link line of every module.
            assertEquals(listOf("SharedKotlin"), targets.getValue("KotlinRuntime").targetDependencies)

            // Every destination sees the API of its own target, and links the matching slice of the Kotlin binary.
            val consumer = projectPath.resolve("consumer")
            consumer.resolve("Sources/Consumer").createDirectories()
            consumer.resolve("Package.swift").writeText(
                """
                // swift-tools-version: 5.9
                import PackageDescription
                let package = Package(
                    name: "Consumer",
                    platforms: [.iOS("18.0"), .macOS("15.0")],
                    dependencies: [.package(name: "Shared", path: "../package/Debug")],
                    targets: [
                        .executableTarget(name: "Consumer", dependencies: [.product(name: "SharedLibrary", package: "Shared")])
                    ]
                )
                """.trimIndent()
            )
            consumer.resolve("Sources/Consumer/main.swift").writeText(
                """
                import Shared
                import Common

                print(sharedApi(), commonApi())
                #if os(macOS)
                print(macosArm64Api())
                #elseif targetEnvironment(simulator)
                print(iosSimulatorArm64Api())
                #else
                print(iosArm64Api())
                #endif
                """.trimIndent()
            )

            SwiftDestination.entries.forEach { destination ->
                val build = consumer.swiftBuild(destination)
                assertTrue(build.isSuccessful, "The consumer of the exported package failed to build for $destination:\n$build")
            }

            // The API of another destination is not there: calling it is a compilation error, not a link error.
            consumer.resolve("Sources/Consumer/main.swift").writeText(
                """
                import Common

                print(iosArm64Api())
                """.trimIndent()
            )
            val macosBuild = consumer.swiftBuild(SwiftDestination.MACOS)
            assertFalse(macosBuild.isSuccessful, "The consumer built for macOS with the iOS API:\n$macosBuild")
            assertEquals(listOf("cannot find 'iosArm64Api' in scope"), macosBuild.output.swiftCompilationErrors())

            build(":exportDebugSwiftPackage") {
                assertTasksUpToDate(
                    ":generateDebugSwiftPackage",
                    ":assembleDebugSwiftPackageBinary",
                    ":exportDebugSwiftPackage",
                )
                assertConfigurationCacheReused()
            }
        }
    }

    @DisplayName("A renamed module leaves nothing of the old one in the exported package")
    @GradleTest
    fun testRenamedModuleLeavesNoStaleFiles(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosSimulatorArm64()
                    sourceSets.commonMain.get().compileSource("fun sharedApi(): Int = 1")
                }
                export.swift {
                    moduleName.set(project.providers.gradleProperty("swiftModuleName").orElse("Before"))
                    swiftPackageIntegration {
                        outputDirectory.set(project.layout.projectDirectory.dir("package"))
                    }
                }
            }

            val exportedPackage = projectPath.resolve("package/Debug")

            build(":exportDebugSwiftPackage")
            val sourcesBefore = exportedPackage.resolve("Sources").directoryNames()
            assertEquals(setOf("Before", "SharedBridge_Before"), sourcesBefore.filter { it.endsWith("Before") }.toSet())
            assertEquals(setOf("BeforeKotlin.xcframework"), exportedPackage.directoryNames().filter { it.endsWith(".xcframework") }.toSet())

            build(":exportDebugSwiftPackage", "-PswiftModuleName=After")
            assertEquals(
                sourcesBefore - setOf("Before", "SharedBridge_Before") + setOf("After", "SharedBridge_After"),
                exportedPackage.resolve("Sources").directoryNames(),
            )
            assertEquals(setOf("AfterKotlin.xcframework"), exportedPackage.directoryNames().filter { it.endsWith(".xcframework") }.toSet())
        }
    }

    @DisplayName("Targets that export different Swift modules can't share a package")
    @GradleTest
    fun testPerTargetApiDependencies(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()
                    macosArm64()
                    sourceSets.commonMain {
                        compileStubSourceWithSourceSetName()
                        dependencies {
                            api(project(":common"))
                        }
                    }
                    sourceSets.getByName("iosArm64Main").dependencies { api(project(":depIos")) }
                    sourceSets.getByName("iosSimulatorArm64Main").dependencies { api(project(":depIosSim")) }
                    sourceSets.getByName("macosArm64Main").dependencies { api(project(":depMacOs")) }
                }
                export.swift {
                    moduleName.set("Shared")
                    swiftPackageIntegration {
                        outputDirectory.set(project.layout.projectDirectory.dir("package"))
                    }
                }
            }

            include(
                project("empty", gradleVersion) {
                    buildScriptInjection {
                        project.applyMultiplatform {
                            iosArm64()
                            iosSimulatorArm64()
                            macosArm64()
                            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                        }
                    }
                },
                "common"
            )
            include(
                project("empty", gradleVersion) {
                    buildScriptInjection {
                        project.applyMultiplatform {
                            iosArm64()
                            sourceSets.commonMain.get().compileSource("fun depIosApi(): Int = 1")
                        }
                    }
                },
                "depIos"
            )
            include(
                project("empty", gradleVersion) {
                    buildScriptInjection {
                        project.applyMultiplatform {
                            iosSimulatorArm64()
                            sourceSets.commonMain.get().compileSource("fun depIosSimApi(): Int = 1")
                        }
                    }
                },
                "depIosSim"
            )
            include(
                project("empty", gradleVersion) {
                    buildScriptInjection {
                        project.applyMultiplatform {
                            macosArm64()
                            sourceSets.commonMain.get().compileSource("fun depMacOsApi(): Int = 1")
                        }
                    }
                },
                "depMacOs"
            )

            // Every target exports a module the other targets don't have: depIos, depIosSim or depMacOs.
            // Combining targets with different Swift modules is not supported, so the export is refused.
            buildAndFail(":exportDebugSwiftPackage") {
                assertHasDiagnostic(KotlinToolingDiagnostics.SwiftExportPackageModulesMismatch)
                assertTasksFailed(":generateDebugSwiftPackage")
                assertNull(task(":exportDebugSwiftPackage"))
            }
            assertDirectoryDoesNotExist(projectPath.resolve("package/Debug"))
        }
    }

    @DisplayName("A module whose Swift dependencies differ per target can't be exported in one package")
    @GradleTest
    fun testPerTargetDependenciesOfAnExportedModule(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()
                    macosArm64()
                    sourceSets.commonMain {
                        compileStubSourceWithSourceSetName()
                        dependencies {
                            api(project(":networking"))
                        }
                    }
                }
                export.swift {
                    moduleName.set("Shared")
                    swiftPackageIntegration {
                        outputDirectory.set(project.layout.projectDirectory.dir("package"))
                    }
                }
            }

            // Exported on every target, but its iOS API exposes a type of an iOS-only library.
            include(
                project("empty", gradleVersion) {
                    buildScriptInjection {
                        project.applyMultiplatform {
                            iosArm64()
                            iosSimulatorArm64()
                            macosArm64()
                            sourceSets.commonMain.get().compileSource("fun networkingApi(): Int = 1")
                            listOf("iosArm64Main", "iosSimulatorArm64Main").forEach { sourceSet ->
                                sourceSets.getByName(sourceSet).apply {
                                    dependencies { api(project(":iosWidgets")) }
                                    compileSource("fun widget(): Widget = Widget(\"Hello\")")
                                }
                            }
                        }
                    }
                },
                "networking"
            )
            include(
                project("empty", gradleVersion) {
                    buildScriptInjection {
                        project.applyMultiplatform {
                            iosArm64()
                            iosSimulatorArm64()
                            sourceSets.commonMain.get().compileSource("class Widget(val title: String)")
                        }
                    }
                },
                "iosWidgets"
            )

            buildAndFail(":exportDebugSwiftPackage") {
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.SwiftExportPackageModulesMismatch,
                    // The library is exported transitively, so its module is named after the root project too.
                    withSubstring = "Networking: depends on [KotlinRuntimeSupport, SharedIosWidgets] for iosArm64 " +
                            "and on [KotlinRuntimeSupport] for macosArm64",
                )
                assertTasksFailed(":generateDebugSwiftPackage")
            }
        }
    }

    @DisplayName("Targets of the same Apple platform are combined into a fat slice of the Kotlin binary")
    @GradleTest
    fun testFatSliceOfTheKotlinBinary(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    // Two simulator targets: one XCFramework slice holds both architectures.
                    iosSimulatorArm64()
                    iosX64()
                    sourceSets.commonMain.get().compileSource("fun sharedApi(): Int = 1")
                }
                export.swift {
                    moduleName.set("Shared")
                    swiftPackageIntegration {
                        outputDirectory.set(project.layout.projectDirectory.dir("package"))
                    }
                }
            }

            build(":exportDebugSwiftPackage")

            val xcframework = projectPath.resolve("package/Debug/SharedKotlin.xcframework")
            assertEquals(setOf("ios-arm64", "ios-arm64_x86_64-simulator"), xcframework.directoryNames())
            assertEquals(setOf("arm64"), xcframework.resolve("ios-arm64/libSharedKotlin.a").architectures())
            assertEquals(setOf("arm64", "x86_64"), xcframework.resolve("ios-arm64_x86_64-simulator/libSharedKotlin.a").architectures())
        }
    }

    /** A destination a Swift package is built for: the target triple and the SDK of `swift build`. */
    private enum class SwiftDestination(val triple: String, val sdk: String) {
        MACOS("arm64-apple-macosx15.0", "macosx"),
        IOS_SIMULATOR("arm64-apple-ios18.0-simulator", "iphonesimulator"),
        IOS("arm64-apple-ios18.0", "iphoneos"),
    }

    /** Builds the Swift package at this path for [destination], in a scratch directory of its own. */
    private fun Path.swiftBuild(destination: SwiftDestination): ProcessRunResult {
        val sdk = runProcess(listOf("xcrun", "--sdk", destination.sdk, "--show-sdk-path"), toFile())
            .also { assertTrue(it.isSuccessful, it.toString()) }
            .output.trim()
        return runProcess(
            listOf("swift", "build", "--triple", destination.triple, "--sdk", sdk, "--scratch-path", ".build-${destination.sdk}"),
            toFile(),
        )
    }

    /** The messages of the Swift compilation errors in a `swift build` output, which colours them. */
    private fun String.swiftCompilationErrors(): List<String> = replace(ansiEscape, "")
        .lineSequence().mapNotNull { swiftError.find(it)?.groupValues?.get(1) }.distinct().toList()

    /** The architectures of a static library, as `lipo` lists them. */
    private fun Path.architectures(): Set<String> {
        val lipo = runProcess(listOf("lipo", "-archs", toString()), parent.toFile())
        assertTrue(lipo.isSuccessful, lipo.toString())
        return lipo.output.trim().split(" ").toSet()
    }

    private fun Path.directoryNames(): Set<String> = listDirectoryEntries().filter { it.isDirectory() }.map { it.name }.toSet()

    private companion object {
        val swiftError = Regex("""\.swift:\d+:\d+: error: (.*)$""")
        val ansiEscape = Regex("""\u001B\[[0-9;]*m""")
    }
}

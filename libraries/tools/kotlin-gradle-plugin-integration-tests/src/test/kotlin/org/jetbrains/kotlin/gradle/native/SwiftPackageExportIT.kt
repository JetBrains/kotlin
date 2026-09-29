/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.test.assertEquals
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

            val exportedPackage = projectPath.resolve("package/Debug")
            assertDirectoryDoesNotExist(exportedPackage.resolve("OtherIncludes"))
            assertEquals(
                setOf("ios-arm64", "ios-arm64-simulator", "macos-arm64"),
                exportedPackage.resolve("SharedKotlin.xcframework").directoryNames(),
            )

            val manifest = exportedPackage.dumpPackage()
            assertEquals(
                mapOf("ios" to "18.0", "macos" to "15.0"),
                manifest.objects("platforms").associate { it.string("platformName") to it.string("version") },
            )
            assertEquals(
                listOf("SharedLibrary"),
                manifest.objects("products").map { it.string("name") },
            )
            val targets = manifest.objects("targets").associateBy { it.string("name") }
            assertEquals("binary", targets.getValue("SharedKotlin").string("type"))
            assertEquals("SharedKotlin.xcframework", targets.getValue("SharedKotlin").string("path"))
            // The Kotlin runtime target is what puts the Kotlin binary on the link line of every module.
            assertEquals(listOf("SharedKotlin"), targets.getValue("KotlinRuntime").dependencyNames())

            val sources = exportedPackage.resolve("Sources")

            // The API of `:common` differs per target, so every target gets a branch of its own.
            assertEquals(
                mapOf(
                    "#if os(iOS) && !targetEnvironment(simulator) && !targetEnvironment(macCatalyst) && arch(arm64)" to
                            setOf("commonApi", "iosArm64Api"),
                    "#elseif os(iOS) && targetEnvironment(simulator) && arch(arm64)" to
                            setOf("commonApi", "iosSimulatorArm64Api"),
                    "#elseif os(macOS) && arch(arm64)" to
                            setOf("commonApi", "macosArm64Api"),
                    "#else" to emptySet(),
                ),
                sources.resolve("Common/Common.swift").publicFunctionsByBranch(),
            )
            assertEquals(
                listOf(
                    "#if TARGET_OS_IOS && !TARGET_OS_SIMULATOR && !TARGET_OS_MACCATALYST && TARGET_CPU_ARM64 && TARGET_RT_64_BIT",
                    "#elif TARGET_OS_IOS && TARGET_OS_SIMULATOR && TARGET_CPU_ARM64 && TARGET_RT_64_BIT",
                    "#elif TARGET_OS_OSX && TARGET_CPU_ARM64 && TARGET_RT_64_BIT",
                    "#else",
                    "#endif",
                ),
                sources.resolve("SharedBridge_Common/include/Common.h").conditionalDirectives(),
            )

            // The API of the root module is the same for every target, so its sources are left as they are.
            assertEquals(emptyList(), sources.resolve("Shared/Shared.swift").conditionalDirectives())
            assertEquals(emptyList(), sources.resolve("SharedBridge_Shared/include/Shared.h").conditionalDirectives())

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

            fun sdkPath(sdk: String) = runProcess(listOf("xcrun", "--sdk", sdk, "--show-sdk-path"), consumer.toFile())
                .also { assertTrue(it.isSuccessful, it.toString()) }
                .output.trim()

            mapOf(
                "arm64-apple-macosx15.0" to "macosx",
                "arm64-apple-ios18.0-simulator" to "iphonesimulator",
                "arm64-apple-ios18.0" to "iphoneos",
            ).forEach { destination ->
                val triple = destination.key
                val sdk = destination.value
                val build = runProcess(
                    listOf("swift", "build", "--triple", triple, "--sdk", sdkPath(sdk), "--scratch-path", ".build-$sdk"),
                    consumer.toFile(),
                )
                assertTrue(build.isSuccessful, "The consumer of the exported package failed to build for $triple:\n$build")
            }

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

    /**
     * The conditional compilation directives of a generated file, in order.
     */
    private fun Path.conditionalDirectives(): List<String> = readLines().filter { line ->
        conditionalDirectivePrefixes.any { line == it || line.startsWith("$it ") }
    }

    /**
     * The names of the public top-level functions of a generated Swift file, by the conditional compilation branch
     * they are declared in.
     */
    private fun Path.publicFunctionsByBranch(): Map<String, Set<String>> {
        val functionsByBranch = LinkedHashMap<String, MutableSet<String>>()
        var branch: String? = null
        readLines().forEach { line ->
            when {
                line == "#endif" -> branch = null
                conditionalDirectivePrefixes.any { line == it || line.startsWith("$it ") } ->
                    branch = line.also { functionsByBranch[it] = linkedSetOf() }
                else -> publicFunction.find(line)?.let { match ->
                    functionsByBranch.getValue(checkNotNull(branch) { "'$line' is outside of any branch" })
                        .add(match.groupValues[1])
                }
            }
        }
        return functionsByBranch
    }

    private fun Path.directoryNames(): Set<String> = listDirectoryEntries().filter { it.isDirectory() }.map { it.name }.toSet()

    /**
     * The manifest of the package at this path as SwiftPM reads it.
     */
    private fun Path.dumpPackage(): JsonObject {
        val dump = runProcess(listOf("swift", "package", "dump-package"), toFile(), redirectErrorStream = false)
        assertTrue(dump.isSuccessful, dump.toString())
        return Json.parseToJsonElement(dump.output).jsonObject
    }

    private fun JsonObject.objects(key: String): List<JsonObject> = getValue(key).jsonArray.map { it.jsonObject }

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

    /**
     * The names of the targets this target of a dumped manifest depends on.
     */
    private fun JsonObject.dependencyNames(): List<String> = objects("dependencies").map { dependency ->
        dependency.getValue("byName").jsonArray.first().jsonPrimitive.content
    }

    private companion object {
        val conditionalDirectivePrefixes = listOf("#if", "#elseif", "#elif", "#else", "#endif")
        val publicFunction = Regex("""^public func (\w+)\(""")
    }
}

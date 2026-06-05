/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream
import kotlin.io.path.*
import kotlin.test.assertEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)

@DisplayName("SwiftPM import XCFrameworks integration")
@SwiftPMImportGradlePluginTests
@GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
class SwiftPMImportXCFrameworksIT : KGPBaseTest() {

    @DisplayName("E2E smoke XCFramework emits a package that can be wired with a generic SwiftPM package and ran on the simulator")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `smoke test XCFrameworks with SwiftPM import dependencies`(
        version: GradleVersion,
        isStatic: Boolean,
    ) {
        sampleProjectWithTransitiveSwiftPMDependencies(version, isStatic) {
            build(":assembleEmptyDebugXCFramework") {
                assertHasDiagnostic(KotlinToolingDiagnostics.XCFrameworkWithSwiftPMDependencies)
            }

            val xcframeworkPath = projectPath.resolve("build/XCFrameworks/debug")
            assertDirectoryExists(xcframeworkPath)

            val consumerPackage = projectPath.resolve("consumerPackage").also { it.createDirectories() }
            val xcframeworkPackagePath = xcframeworkPath.relativeTo(consumerPackage)

            runProcess(listOf("/usr/bin/swift", "package", "init"), workingDir = consumerPackage.toFile())
            consumerPackage.resolve("Package.swift").writeText(
                """
                        // swift-tools-version: 5.9
                        import PackageDescription

                        let package = Package(
                            name: "consumerPackage",
                            platforms: [
                                .iOS(.v15)
                            ],
                            products: [
                                .executable(
                                    name: "consumerPackage",
                                    targets: ["consumerPackage"]
                                ),
                            ],
                            dependencies: [
                                .package(path: "${xcframeworkPackagePath.pathString}")
                            ],
                            targets: [
                                .target(
                                    name: "consumerPackage",
                                    dependencies: [.product(name: "emptyPackage", package: "${xcframeworkPackagePath.name}")]
                                ),
                            ]
                        )
                    """.trimIndent()
            )
            consumerPackage.resolve("Sources/consumerPackage/main.swift").writeText(
                """
                        import empty
                        
                        KotlinClass().callSubproject()
                    """.trimIndent()
            )

            XCTestHelpers().use {
                val simulator = it.createSimulator()

                val ddPath = projectPath.resolve("dd")
                runProcess(listOf("xcodebuild", "build", "-scheme", "consumerPackage", "-destination", "id=${simulator.udid}", "-derivedDataPath", ddPath.pathString), workingDir = consumerPackage.toFile())

                simulator.boot()
                val executablePath = ddPath.resolve("Build/Products/Debug-iphonesimulator/consumerPackage")
                assertFileExists(executablePath)

                val stdout = projectPath.resolve("stdout").toFile()
                val stderr = projectPath.resolve("stderr").toFile()
                simulator.spawn(
                    executablePath,
                    mapOf(
                        // This is passed by Xcode when it runs the executable. Technically this is only necessary with dynamic linkage
                        "SIMCTL_CHILD_DYLD_FRAMEWORK_PATH" to executablePath.parent.pathString,
                    ),
                    stdout,
                    stderr
                )

                assertEquals(
                    """
                            Kotlin
                            Swift
                            
                        """.trimIndent(),
                    stdout.readText(),
                )

                assertEquals(
                    "",
                    stderr.readText(),
                )
            }
        }
    }

    private fun sampleProjectWithTransitiveSwiftPMDependencies(
        version: GradleVersion,
        isStatic: Boolean,
        assertions: TestProject.() -> Unit
    ) {
        project("empty", version) {
            val cache = projectPath.resolve("customXcodePackageCache").toFile()

            plugins {
                kotlin("multiplatform")
            }
            withLockFileFixture {
                val packageDependencyRepo = repoRef("packageDependency").also {
                    createRepo(
                        name = it.name,
                        tags = emptyList(),
                        source = """
                            import Foundation
                            
                            @objc public class SwiftClass : NSObject {
                                @objc public func callSwift() {
                                    print("Swift")
                                }
                            }
                        """.trimIndent()
                    )
                }

                val subprojectName = "subproject"
                val subproject = project("empty", version) {
                    buildScriptInjection {
                        project.applyMultiplatform {
                            configureSwiftPmTestArgs(cache)

                            iosSimulatorArm64()
                            iosX64()
                            iosArm64()

                            sourceSets.commonMain.get().compileSource(
                                """
                            import swiftPMImport.empty.subproject.*
    
                            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                            fun callIntoSwift() {
                                println("Kotlin")
                                SwiftClass().callSwift()
                            }
                            """.trimIndent()
                            )

                            swiftPMDependencies {
                                swiftPackage(
                                    url = url(packageDependencyRepo.url),
                                    version = branch("master"),
                                    products = listOf(product(packageDependencyRepo.name)),
                                )
                            }
                        }
                    }
                }
                include(subproject, subprojectName)

                buildScriptInjection {
                    project.applyMultiplatform {
                        configureSwiftPmTestArgs(cache)

                        val xcf = project.XCFramework()
                        listOf(
                            iosSimulatorArm64(),
                            iosX64(),
                            iosArm64(),
                        ).forEach {
                            it.binaries.framework {
                                this.isStatic = isStatic
                                xcf.add(this)
                            }
                        }

                        sourceSets.commonMain.get().dependencies {
                            implementation(project(":${subprojectName}"))
                        }
                        sourceSets.commonMain.get().compileSource(
                            """
                            class KotlinClass {
                                fun callSubproject() {
                                    callIntoSwift()
                                }
                            }
                            """.trimIndent()
                        )
                    }
                }

                assertions()
            }
        }
    }

    private class SpmImportArgumentsProvider : GradleArgumentsProvider() {
        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
            return super.provideArguments(context).flatMap { arguments ->
                val gradleVersion = arguments.get().first()
                Stream.of(true, false).map { isStatic ->
                    Arguments.of(gradleVersion, isStatic)
                }
            }
        }
    }

}

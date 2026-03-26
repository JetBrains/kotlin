/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(EnvironmentalVariablesOverride::class, ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString
import kotlin.io.resolve
import kotlin.test.assertEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class SwiftPMImportDynamicLinkageTests : KGPBaseTest() {

    @GradleTest
    fun `dynamic linkage with SwiftPM import - promotes sources-based transitive SwiftPM dependencies to dynamic libraries`(version: GradleVersion) {
        project("emptyxcode", version) {
            val packageDependency = projectPath.resolve("packageDependency").also { it.createDirectories() }.toFile()
            runProcess(listOf("swift", "package", "init", "--type", "library"), packageDependency)
            val swiftClassName = "LocalHelper"

            packageDependency.resolve("Sources/packageDependency/packageDependency.swift").writeText(
                """
                    import Foundation
                    @objc public class ${swiftClassName}: NSObject {
                        @objc public static func greeting(_ arg: String) {
                            print("Hello from \(arg)")
                        }
                    }
                """.trimIndent()
            )
            projectPath.resolve("iosApp/iosApp/iOSApp.swift").toFile().writeText(
                """
                    import packageDependency
                    import Shared
                    
                    @main
                    struct iOSApp {
                        static func main() {
                            ${swiftClassName}.greeting("Swift")
                            KotlinConsumer().consumePackage()
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
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = false
                        }
                    }

                    sourceSets.commonMain.get().compileSource(
                        """
                            import swiftPMImport.emptyxcode.${swiftClassName}
                                
                             class KotlinConsumer {
                                 fun consumePackage() {
                                    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                                    ${swiftClassName}.greeting("Kotlin")
                                 }
                             }
                        """.trimIndent()
                    )

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir("packageDependency"),
                            products = listOf("packageDependency"),
                        )
                    }
                }
            }

            build(
                "integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj"
                )
            )

            val derivedDataPath = projectPath.resolve("dd")
            buildXcodeProject(
                xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
                action = XcodeBuildAction.Build,
                destination = "generic/platform=iOS Simulator",
                buildSettingOverrides = mapOf(
                    "ARCHS" to "arm64",
                ),
                derivedDataPath = derivedDataPath,
            )

            val appPath = derivedDataPath.resolve("Build/Products/Debug-iphonesimulator/emptyxcode.app")

            checkBinariesLinkageToPromotedPackageDependency(appPath)
            checkApplicationRunsAndObjCRuntimeIsHappy(projectPath, appPath)
        }
    }

    /**
     * Check that:
     * - packageDependency binary got promoted
     * - implementation of packageDependency is only in the packageDependency binary
     * - everyone using packageDependency is bound to the packageDependency dynamic library
     */
    private fun checkBinariesLinkageToPromotedPackageDependency(appPath: Path) {
        fun grepSymbols(binary: Path, symbolsContainingAnyOf: List<String>) = ProcessBuilder.startPipeline(
            listOf(
                ProcessBuilder(listOf("nm", "-m", binary.pathString)),
                ProcessBuilder(listOf("swift", "demangle")),
            ).map {
                it.redirectError(ProcessBuilder.Redirect.INHERIT)
            }
        ).last().inputReader().lineSequence().filter { line ->
            symbolsContainingAnyOf.any { it in line }
        }.map {
            val addressPrefix = 17
            it.drop(addressPrefix)
        }.toSet()

        val packageDependencySymbols = listOf(
            "_OBJC_CLASS_\$_packageDependency.LocalHelper",
            "packageDependency.LocalHelper.greeting",
        )

        val swiftClassNameSymbols = appPath.resolve("Frameworks").toFile().listFiles().associate {
            assert(it.extension == "framework")
            val binaryName = it.nameWithoutExtension
            binaryName to grepSymbols(
                it.resolve(binaryName).toPath(),
                packageDependencySymbols
            )
        }
        val packageDependencyBinaryName = appPath.resolve("Frameworks").toFile().listFiles().single { it.name.startsWith("packageDependency") }.nameWithoutExtension

        assertEquals(
            mapOf(
                "PromoteKMPDependenciesToDynamicLibraries" to setOf(
                ),
                "Shared" to setOf(
                    "(undefined) external _OBJC_CLASS_\$_packageDependency.LocalHelper (from ${packageDependencyBinaryName})",
                ),
                packageDependencyBinaryName to setOf(
                    "(__TEXT,__text) external static packageDependency.LocalHelper.greeting(Swift.String) -> ()",
                    "(__TEXT,__text) non-external @objc static packageDependency.LocalHelper.greeting(Swift.String) -> ()",
                    "(__DATA,__objc_data) external _OBJC_CLASS_\$_packageDependency.LocalHelper",
                ),
            ).prettyPrinted,
            swiftClassNameSymbols.prettyPrinted,
        )

        val appSymbols = grepSymbols(
            appPath.resolve("emptyxcode.debug.dylib"),
            packageDependencySymbols
        )
        assertEquals(
            setOf(
                "(undefined) external static packageDependency.LocalHelper.greeting(Swift.String) -> () (from ${packageDependencyBinaryName})"
            ).prettyPrinted,
            appSymbols.prettyPrinted,
        )
    }

    /**
     * Run the app to make sure we don't explode at load time and that we don't see ObjC runtime complaining about some duplication in stderr
     */
    private fun checkApplicationRunsAndObjCRuntimeIsHappy(projectPath: Path, appPath: Path) {
        val stdout = projectPath.resolve("stdout").toFile()
        val stderr = projectPath.resolve("stderr").toFile()

        XCTestHelpers().use {
            val simulator = it.createSimulator().apply {
                boot()
            }

            simulator.install(appPath.toFile())
            simulator.launch("org.example.project.emptyxcode", stdout, stderr)
        }

        assertEquals(
            """
                Hello from Swift
                Hello from Kotlin
                """.trimIndent(),
            stdout.readLines()
                // the last line is always "bundleId: pid"
                .dropLast(1).joinToString("\n"),
        )
        // ObjC runtime emits warnings such as those about duplicate classes in stderr
        assertEquals(
            "",
            stderr.readText()
        )
    }

}
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
import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString
import kotlin.io.resolve
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class SwiftPMImportDynamicLinkageTests : KGPBaseTest() {

    /**
     * This is not the behavior we want, but with static frameworks this can unfortunately happen.
     *
     * In this test whether or not duplication will happen depends on alphabetic ordering of Shared and FrameworkTarget because both get
     * autolinked and autolinking seems to add instructions happen alphabetically.
     */
    @GradleTest
    fun `dynamic linkage with SwiftPM import - duplicates static binary during application linkage`(version: GradleVersion) {
        project("emptyxcode", version) {
            projectPath.resolve("iosApp/FrameworkTarget/FrameworkTarget.h").toFile().writeText(
                """
                    #import <Foundation/Foundation.h>
                    
                    @interface SharedType : NSObject
                    @end
                    
                    @interface ApplicationOnlyType : NSObject
                    @end
                """.trimIndent()
            )
            projectPath.resolve("iosApp/FrameworkTarget/SharedType.m").toFile().writeText(
                """
                    #import "FrameworkTarget.h"
                    @implementation SharedType
                    - (NSString *)description { return @"SharedType"; };
                    @end
                """.trimIndent()
            )
            projectPath.resolve("iosApp/FrameworkTarget/ApplicationOnlyType.m").toFile().writeText(
                """
                    #import "FrameworkTarget.h"
                    @implementation ApplicationOnlyType
                    - (NSString *)description { return @"ApplicationOnlyType"; };
                    @end
                """.trimIndent()
            )

            val outgoingArchive = projectPath.resolve("FrameworkTarget.xcarchive")
            runProcess(
                listOf(
                    "xcodebuild", "archive",
                    "-project", "iosApp.xcodeproj",
                    "-scheme", "FrameworkTarget",
                    "-destination", "generic/platform=iOS Simulator",
                    "-archivePath", outgoingArchive.pathString,
                    "ARCHS=arm64", "SKIP_INSTALL=NO", "MACH_O_TYPE=staticlib",
                ),
                projectPath.resolve("iosApp").toFile(),
            ).assertProcessRunResult { assertTrue(isSuccessful) }

            val packageDependency = projectPath.resolve("PackageDependency").also { it.createDirectories() }.toFile()
            runProcess(listOf("/usr/bin/swift", "package", "init", "--type", "library"), packageDependency)
            val outgoingXCFramework = packageDependency.resolve("FrameworkTarget.xcframework")
            runProcess(
                listOf(
                    "xcodebuild", "-create-xcframework",
                    "-archive", outgoingArchive.pathString,
                    "-framework", "FrameworkTarget.framework",
                    "-output", outgoingXCFramework.path,
                ),
                projectPath.resolve("iosApp").toFile(),
            ).assertProcessRunResult { assertTrue(isSuccessful) }

            packageDependency.resolve("Package.swift").writeText(
                """
                    // swift-tools-version: 5.9
                    import PackageDescription

                    let package = Package(
                        name: "PackageDependency",
                        products: [
                            .library(
                                name: "PackageDependency",
                                targets: ["PackageDependency"]
                            ),
                        ],
                        targets: [
                            .target(
                                name: "PackageDependency",
                                dependencies: [
                                    "FrameworkTarget"
                                ]
                            ),
                            .binaryTarget(
                                name: "FrameworkTarget",
                                path: "${outgoingXCFramework.name}"
                            )
                        ]
                    )
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
                            import swiftPMImport.emptyxcode.SharedType
                                
                             class KotlinConsumer {
                                 fun consumePackage() {
                                    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                                    println(SharedType())
                                 }
                             }
                        """.trimIndent()
                    )

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir("PackageDependency"),
                            products = listOf("PackageDependency"),
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

            projectPath.resolve("iosApp/iosApp/iOSApp.swift").toFile().writeText(
                """
                    import FrameworkTarget
                    import Shared
                    
                    @main
                    struct iOSApp {
                        static func main() {
                            KotlinConsumer().consumePackage()
                            print(SharedType())
                            print(ApplicationOnlyType())
                        }
                    }
                """.trimIndent()
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
            checkBinariesLinkage(
                appPath,
                symbolsFilter = listOf(
                    "_OBJC_CLASS_\$_SharedType",
                    "_OBJC_CLASS_\$_ApplicationOnlyType",
                ),
                expectedLibrarySymbols = mapOf(
                    "FrameworkTarget" to setOf(
                    ),
                    "KotlinMultiplatformLinkedPackageDylib" to setOf(
                    ),
                    "Shared" to setOf(
                        "(__DATA,__objc_data) external _OBJC_CLASS_\$_SharedType",
                    ),
                ),
                expectedApplicationSymbols = setOf(
                    "(__DATA,__objc_data) external _OBJC_CLASS_\$_ApplicationOnlyType",
                    "(__DATA,__objc_data) external _OBJC_CLASS_\$_SharedType",
                ),
            )

            val result = runApplication(projectPath = projectPath, appPath = appPath)
            assertContains(result.stderr.readLines().single(), "Class SharedType is implemented in both")
        }
    }

    @GradleTest
    fun `dynamic linkage with SwiftPM import - exposes sources-based transitive SwiftPM dependencies through Kotlin dynamic library`(version: GradleVersion) {
        project("emptyxcode", version) {
            val packageDependency = projectPath.resolve("PackageDependency").also { it.createDirectories() }.toFile()
            runProcess(listOf("/usr/bin/swift", "package", "init", "--type", "library"), packageDependency)
            val swiftClassName = "LocalHelper"

            packageDependency.resolve("Sources/PackageDependency/PackageDependency.swift").writeText(
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
                    import PackageDependency
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
                            directory = project.layout.projectDirectory.dir("PackageDependency"),
                            products = listOf("PackageDependency"),
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

            /**
             * Check that:
             * - PackageDependency was exposed through KotlinMultiplatformLinkedPackageDylib
             * - implementation of PackageDependency is only in the KotlinMultiplatformLinkedPackageDylib binary
             * - everyone using PackageDependency is bound to the KotlinMultiplatformLinkedPackageDylib dynamic library
             */
            checkBinariesLinkage(
                appPath,
                symbolsFilter = listOf(
                    "_OBJC_CLASS_\$_PackageDependency.LocalHelper",
                    "PackageDependency.LocalHelper.greeting",
                ),
                expectedLibrarySymbols = mapOf(
                    "Shared" to setOf(
                        "(undefined) external _OBJC_CLASS_\$_PackageDependency.LocalHelper (from KotlinMultiplatformLinkedPackageDylib)",
                    ),
                    "KotlinMultiplatformLinkedPackageDylib" to setOf(
                        "(__TEXT,__text) external static PackageDependency.LocalHelper.greeting(Swift.String) -> ()",
                        "(__TEXT,__text) non-external @objc static PackageDependency.LocalHelper.greeting(Swift.String) -> ()",
                        "(__DATA,__objc_data) external _OBJC_CLASS_\$_PackageDependency.LocalHelper",
                    ),
                ),
                expectedApplicationSymbols = setOf(
                    "(undefined) external static PackageDependency.LocalHelper.greeting(Swift.String) -> () (from KotlinMultiplatformLinkedPackageDylib)"
                ),
            )
            checkApplicationRun(projectPath, appPath)
        }
    }

    private fun checkBinariesLinkage(
        appPath: Path,
        symbolsFilter: List<String>,
        expectedLibrarySymbols: Map<String, Set<String>>,
        expectedApplicationSymbols: Set<String>,
    ) {
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

        val swiftClassNameSymbols = appPath.resolve("Frameworks").toFile().listFiles().associate {
            assert(it.extension == "framework")
            val binaryName = it.nameWithoutExtension
            binaryName to grepSymbols(
                it.resolve(binaryName).toPath(),
                symbolsFilter,
            )
        }
        val appSymbols = grepSymbols(
            appPath.resolve("emptyxcode.debug.dylib"),
            symbolsFilter,
        )

        assertEquals(
            expectedLibrarySymbols.prettyPrinted,
            swiftClassNameSymbols.prettyPrinted,
        )
        assertEquals(
            expectedApplicationSymbols.prettyPrinted,
            appSymbols.prettyPrinted,
        )
    }

    /**
     * Run the app to make sure we don't explode at load time and that we don't see ObjC runtime complaining about some duplication in stderr
     */
    private fun checkApplicationRun(projectPath: Path, appPath: Path) {
        val result = runApplication(projectPath = projectPath, appPath = appPath)

        assertEquals(
            """
                Hello from Swift
                Hello from Kotlin
                """.trimIndent(),
            result.stdout.readLines()
                // the last line is always "bundleId: pid"
                .dropLast(1).joinToString("\n"),
        )
        // ObjC runtime emits warnings such as those about duplicate classes in stderr
        assertEquals(
            "",
            result.stderr.readText()
        )
    }

}
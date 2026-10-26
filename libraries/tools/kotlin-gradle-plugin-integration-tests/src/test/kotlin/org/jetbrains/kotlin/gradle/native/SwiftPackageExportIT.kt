/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.apple.SwiftPackageVersionRange
import org.jetbrains.kotlin.gradle.apple.addSwiftPmGitTag
import org.jetbrains.kotlin.gradle.apple.configureSwiftPmTestArgs
import org.jetbrains.kotlin.gradle.apple.createLocalSwiftPackage
import org.jetbrains.kotlin.gradle.apple.createRepo
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.apple.describeSwiftPackage
import org.jetbrains.kotlin.gradle.apple.dumpSwiftPackage
import org.jetbrains.kotlin.gradle.apple.repoDir
import org.jetbrains.kotlin.gradle.apple.repoRef
import org.jetbrains.kotlin.gradle.apple.targetDependencyPlatforms
import org.jetbrains.kotlin.gradle.apple.withLockFileFixture
import org.jetbrains.kotlin.gradle.apple.writePackageManifest
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PackageResolvedSynchronization
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.util.ProcessRunResult
import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            val consumer = consumerOf(
                exportedPackage,
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
                """
            )
            consumer.assertBuildsForEveryDestination()

            // The API of another destination is not there: calling it is a compilation error, not a link error.
            consumer.resolve("Sources/Consumer/main.swift").writeText(
                """
                import Common

                print(iosArm64Api())
                """.trimIndent()
            )
            consumer.swiftBuild(SwiftDestination.MACOS).assertProcessRunResult {
                assertFalse(isSuccessful, "The consumer built for macOS with the iOS API")
                assertEquals(listOf("cannot find 'iosArm64Api' in scope"), output.swiftCompilationErrors())
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

    @DisplayName("Targets that export different Swift modules share a package")
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

            // Every target exports a module the others don't have: depIos, depIosSim or depMacOs. The package has
            // all three, and each one is an empty module on the other destinations.
            build(":exportDebugSwiftPackage")

            val exportedPackage = projectPath.resolve("package/Debug")
            assertEquals(
                setOf("DepIos", "DepIosSim", "DepMacOs"),
                describeSwiftPackage(exportedPackage).products.single().targets.filter { it.startsWith("Dep") }.toSet(),
            )

            val consumer = consumerOf(
                exportedPackage,
                """
                #if os(macOS)
                import DepMacOs
                print(depMacOsApi())
                #elseif targetEnvironment(simulator)
                import DepIosSim
                print(depIosSimApi())
                #else
                import DepIos
                print(depIosApi())
                #endif
                """
            )
            consumer.assertBuildsForEveryDestination()
        }
    }

    @DisplayName("A dependency that only one Apple platform has is limited to that platform")
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

            build(":exportDebugSwiftPackage")

            // The library is exported transitively, so its module is named after the root project too.
            val exportedPackage = projectPath.resolve("package/Debug")
            assertEquals(
                mapOf("KotlinRuntimeSupport" to null, "SharedIosWidgets" to listOf("ios")),
                dumpSwiftPackage(exportedPackage).targetDependencyPlatforms("Networking")
                    .filterKeys { it == "KotlinRuntimeSupport" || it == "SharedIosWidgets" },
            )

            val consumer = consumerOf(
                exportedPackage,
                """
                import Networking

                print(networkingApi())
                #if os(iOS)
                import SharedIosWidgets
                print(widget().title)
                #endif
                """
            )
            consumer.assertBuildsForEveryDestination()
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
            assertEquals(setOf("arm64"), machOArchitectures(xcframework.resolve("ios-arm64/libSharedKotlin.a")))
            assertEquals(setOf("arm64", "x86_64"), machOArchitectures(xcframework.resolve("ios-arm64_x86_64-simulator/libSharedKotlin.a")))
        }
    }

    @DisplayName("A consumer of the Release package links in an optimised build")
    @GradleTest
    fun testReleasePackageLinksInOptimisedConsumer(
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
                    sourceSets.commonMain.get().compileSource("fun sharedApi(): Int = 1")
                }
                export.swift {
                    moduleName.set("Shared")
                    swiftPackageIntegration {
                        outputDirectory.set(project.layout.projectDirectory.dir("package"))
                    }
                }
            }

            build(":exportReleaseSwiftPackage") {
                assertTasksExecuted(":exportReleaseSwiftPackage")
            }

            val consumer = consumerOf(
                projectPath.resolve("package/Release"),
                """
                import Shared

                print(sharedApi())
                """,
            )
            // KT-90738: an optimised build of KotlinRuntimeSupport lost the link anchor the Kotlin binary references.
            listOf(SwiftDestination.IOS, SwiftDestination.IOS_SIMULATOR).forEach { destination ->
                val build = consumer.swiftBuild(destination, extraArguments = listOf("-c", "release"))
                assertTrue(build.isSuccessful, "The Release consumer failed to build for $destination:\n$build\n${build.output}")
            }
        }
    }

    @DisplayName("The packages of the SwiftPM import are dependencies of the exported package")
    @GradleTest
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun testSwiftPMImportPackagesAreDeclared(
        gradleVersion: GradleVersion,
    ) {
        project("emptyxcode", gradleVersion) {
            val localPackage = projectPath.resolve("../localSwiftPackage")
            createLocalSwiftPackage(localPackage, packageName = "LocalSwiftPackage")

            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosSimulatorArm64()
                    // An imported type in the API, so the exported Swift needs the imported module itself.
                    sourceSets.appleMain.get().compileSource(
                        """
                            @file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                            import swiftPMImport.shared.LocalHelper
                            fun roundTrip(helper: LocalHelper): LocalHelper = helper
                            fun localGreeting(): String = LocalHelper.greeting()
                        """.trimIndent()
                    )
                    with(swiftImport) {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir("../localSwiftPackage"),
                            products = listOf("LocalSwiftPackage"),
                        )
                    }
                }
                export.swift {
                    moduleName.set("Shared")
                    swiftPackageIntegration {
                        outputDirectory.set(project.layout.projectDirectory.dir("package"))
                    }
                }
            }

            build(":exportDebugSwiftPackage") {
                assertTasksExecuted(
                    ":cinteropSwiftPMImportIosSimulatorArm64",
                    ":iosSimulatorArm64SwiftPackageExport",
                    ":exportDebugSwiftPackage",
                )
            }

            val exportedPackage = projectPath.resolve("package/Debug")
            assertContains(exportedPackage.resolve("Sources/Shared/Shared.swift").imports(), "LocalSwiftPackage")
            // Relative to the exported package, so it stays valid when the project moves.
            assertEquals(listOf("../../../localSwiftPackage"), exportedPackage.resolve("Package.swift").localPackagePaths())

            val manifest = describeSwiftPackage(exportedPackage)
            assertEquals("6.1", manifest.toolsVersion)
            val dependency = manifest.dependencies.single()
            assertEquals("fileSystem", dependency.type)
            assertEquals(localPackage.toRealPath(), Path(checkNotNull(dependency.path)).toRealPath())
            assertEquals(
                listOf("LocalSwiftPackage"),
                manifest.targets.single { it.name == "Shared" }.productDependencies,
            )

            // The consumer gets the imported package from the manifest and links it next to the Kotlin binary.
            val consumer = projectPath.resolve("consumer")
            consumer.resolve("Sources/Consumer").createDirectories()
            consumer.resolve("Package.swift").writeText(
                """
                // swift-tools-version: 6.1
                import PackageDescription
                let package = Package(
                    name: "Consumer",
                    platforms: [.iOS("18.0")],
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
                import LocalSwiftPackage

                print(localGreeting(), roundTrip(helper: LocalHelper()))
                """.trimIndent()
            )
            val build = consumer.swiftBuild(SwiftDestination.IOS_SIMULATOR)
            assertTrue(build.isSuccessful, "The consumer of the exported package failed to build:\n$build\n${build.output}")
        }
    }

    @DisplayName("Every Kotlin dependency declares the packages it imports in a package of its own")
    @GradleTest
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun testSwiftPMImportPackagesOfTwoDependencies(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            withLockFileFixture(packageResolvedSynchronization = PackageResolvedSynchronization.None) {
                val lib = repoRef("Lib")

                createRepo(name = lib.name, tags = emptyList(), products = listOf("CalledByA", "CalledByB"))
                val libRepository = repoDir(lib.name)
                addSwiftPmGitTag(
                    libRepository,
                    tag = "1.0.0",
                    files = mapOf(
                        "Sources/CalledByA/CalledByA.swift" to swiftHelper("CalledByA", "calledByA"),
                        "Sources/CalledByB/CalledByB.swift" to swiftHelper("CalledByB", "calledByB"),
                    ),
                )
                // 1.5.0 drops CalledByB.
                libRepository.resolve("Sources/CalledByB").toFile().deleteRecursively()
                writePackageManifest(libRepository, packageName = "Lib", products = listOf("CalledByA"))
                addSwiftPmGitTag(libRepository, tag = "1.5.0")
                val packageCache = cacheDirFile

                plugins {
                    kotlin("multiplatform")
                }
                settingsBuildScriptInjection {
                    settings.rootProject.name = "shared"
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosSimulatorArm64()
                        configureSwiftPmTestArgs(packageCache)
                        with(swiftImport) {
                            packageResolvedSynchronization = noSynchronization()
                        }
                        sourceSets.commonMain {
                            compileSource("fun sharedApi(): String = depAApi() + depBApi()")
                            dependencies {
                                api(project(":depA"))
                                api(project(":depB"))
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

                fun includeDependency(
                    dependencyName: String,
                    productName: String,
                    version: SwiftPMImportExtension.() -> SwiftPMDependency.Remote.Version,
                ) {
                    val dependency = project("empty", gradleVersion) {
                        buildScriptInjection {
                            project.applyMultiplatform {
                                iosSimulatorArm64()
                                configureSwiftPmTestArgs(packageCache)
                                sourceSets.appleMain.get().compileSource(
                                    """
                                    @file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                                    import swiftPMImport.shared.$dependencyName.${productName}Helper
                                    fun ${dependencyName}Api(): String = ${productName}Helper.${productName.replaceFirstChar { it.lowercase() }}()
                                    """.trimIndent()
                                )
                                with(swiftImport) {
                                    packageResolvedSynchronization = noSynchronization()
                                    swiftPackage(
                                        url = url(lib.url),
                                        version = version(),
                                        products = listOf(product(productName)),
                                        packageName = "Lib",
                                    )
                                }
                            }
                        }
                    }
                    include(dependency, dependencyName)
                }
                includeDependency("depA", "CalledByA") { from("1.0.0") }
                includeDependency("depB", "CalledByB") { exact("1.0.0") }

                build(":exportDebugSwiftPackage") {
                    assertTasksExecuted(
                        ":depA:serializeSwiftPMDependenciesMetadata",
                        ":depB:serializeSwiftPMDependenciesMetadata",
                        ":generateDebugSwiftPackage",
                    )
                }

                val exportedPackage = projectPath.resolve("package/Debug")
                val manifest = describeSwiftPackage(exportedPackage)
                // One subpackage per dependency, no direct declaration of Lib.
                assertEquals(
                    listOf(
                        "_depa" to exportedPackage.resolve("subpackages/_depA").toRealPath(),
                        "_depb" to exportedPackage.resolve("subpackages/_depB").toRealPath(),
                    ),
                    manifest.dependencies.map { it.identity to Path(checkNotNull(it.path)).toRealPath() },
                )
                assertEquals(
                    listOf("_depA", "_depB"),
                    manifest.targets.single { it.name == "Shared" }.productDependencies,
                )

                val depAPackage = describeSwiftPackage(exportedPackage.resolve("subpackages/_depA"))
                val depADeclaration = depAPackage.dependencies.single()
                assertEquals("lib", depADeclaration.identity)
                assertEquals(lib.url, depADeclaration.url)
                assertEquals(
                    listOf(SwiftPackageVersionRange("1.0.0", "2.0.0")),
                    checkNotNull(depADeclaration.requirement?.range),
                )
                assertEquals(listOf("CalledByA"), depAPackage.targets.single { it.name == "_depA" }.productDependencies)

                val depBPackage = describeSwiftPackage(exportedPackage.resolve("subpackages/_depB"))
                assertEquals(listOf("1.0.0"), checkNotNull(depBPackage.dependencies.single().requirement?.exact))
                assertEquals(listOf("CalledByB"), depBPackage.targets.single { it.name == "_depB" }.productDependencies)

                val consumer = projectPath.resolve("consumer")
                consumer.resolve("Sources/Consumer").createDirectories()
                consumer.resolve("Package.swift").writeText(
                    """
                    // swift-tools-version: 6.1
                    import PackageDescription
                    let package = Package(
                        name: "Consumer",
                        platforms: [.iOS("18.0")],
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

                    print(sharedApi())
                    """.trimIndent()
                )
                val build = consumer.swiftBuild(
                    SwiftDestination.IOS_SIMULATOR,
                    extraArguments = listOf("--resolver-fingerprint-checking", "warn", "--cache-path", packageCache.path),
                )
                assertEquals(emptyList(), build.output.missingProducts())
                assertTrue(build.isSuccessful, "The consumer of the exported package failed to build:\n$build\n${build.output}")
            }
        }
    }

    /** A destination a Swift package is built for: the target triple and the SDK of `swift build`. */
    private enum class SwiftDestination(val triple: String, val sdk: String) {
        MACOS("arm64-apple-macosx15.0", "macosx"),
        IOS_SIMULATOR("arm64-apple-ios18.0-simulator", "iphonesimulator"),
        IOS("arm64-apple-ios18.0", "iphoneos"),
    }

    /** A consumer package that depends on the exported package at [packagePath] and runs [main]. */
    private fun TestProject.consumerOf(packagePath: Path, main: String): Path {
        val consumer = projectPath.resolve("consumer")
        consumer.resolve("Sources/Consumer").createDirectories()
        consumer.resolve("Package.swift").writeText(
            """
            // swift-tools-version: 6.1
            import PackageDescription
            let package = Package(
                name: "Consumer",
                platforms: [.iOS("18.0"), .macOS("15.0")],
                dependencies: [.package(name: "Shared", path: "${packagePath.toAbsolutePath()}")],
                targets: [
                    .executableTarget(name: "Consumer", dependencies: [.product(name: "SharedLibrary", package: "Shared")])
                ]
            )
            """.trimIndent()
        )
        consumer.resolve("Sources/Consumer/main.swift").writeText(main.trimIndent())
        return consumer
    }

    /** Builds the consumer package at this path for every [SwiftDestination]. */
    private fun Path.assertBuildsForEveryDestination() {
        SwiftDestination.entries.forEach { destination ->
            swiftBuild(destination).assertProcessRunResult {
                assertTrue(isSuccessful, "The consumer of the exported package failed to build for $destination")
            }
        }
    }

    /** Builds the Swift package at this path for [destination], in a scratch directory of its own. */
    private fun Path.swiftBuild(
        destination: SwiftDestination,
        extraArguments: List<String> = emptyList(),
    ): ProcessRunResult {
        val sdk = runProcess(listOf("xcrun", "--sdk", destination.sdk, "--show-sdk-path"), toFile())
            .also { it.assertProcessRunResult { assertTrue(isSuccessful, "No SDK path for ${destination.sdk}") } }
            .output.trim()
        return runProcess(
            listOf("swift", "build", "--triple", destination.triple, "--sdk", sdk, "--scratch-path", ".build-${destination.sdk}") +
                extraArguments,
            toFile(),
        )
    }

    /** The messages of the Swift compilation errors in a `swift build` output, which colours them. */
    private fun String.swiftCompilationErrors(): List<String> = replace(ansiEscape, "")
        .lineSequence().mapNotNull { swiftError.find(it)?.groupValues?.get(1) }.distinct().toList()

    /** The modules a Swift file imports. */
    private fun Path.imports(): List<String> = readLines().mapNotNull { swiftImport.find(it)?.groupValues?.get(1) }

    /** The paths of the local packages a manifest declares, as written. */
    private fun Path.localPackagePaths(): List<String> = readLines().mapNotNull { localPackageEntry.find(it)?.groupValues?.get(1) }

    private fun Path.directoryNames(): Set<String> = listDirectoryEntries().filter { it.isDirectory() }.map { it.name }.toSet()

    private fun swiftHelper(product: String, function: String): String = """
        import Foundation

        @objc public class ${product}Helper: NSObject {
            @objc public static func $function() -> String {
                return "$product.$function"
            }
        }
    """.trimIndent() + "\n"

    /** The products `swift build` reports missing from the package that should have them. */
    private fun String.missingProducts(): List<String> = replace(ansiEscape, "")
        .lineSequence().mapNotNull { missingProduct.find(it)?.groupValues?.get(1) }.distinct().toList()

    private companion object {
        val swiftError = Regex("""\.swift:\d+:\d+: error: (.*)$""")
        val ansiEscape = Regex("""\u001B\[[0-9;]*m""")
        val swiftImport = Regex("""^import (\w+)$""")
        val localPackageEntry = Regex("""\.package\(path: "([^"]+)"\)""")
        val missingProduct = Regex("""error: .*product '([^']+)' required by package""")
    }
}

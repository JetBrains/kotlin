/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PackageResolvedSynchronization
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FingerprintXcodeBuild
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.assertTasksUpToDate
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class FingerprintXcodeBuildTests : KGPBaseTest() {

    private val fingerprintXcodebuildIphoneSimulatorTask =
        ":${lowerCamelCaseName(FingerprintXcodeBuild.TASK_NAME, "iphonesimulator")}"

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    fun `local Swift package source content change does not invalidate prepare xcodebuild dump fingerprint task`(version: GradleVersion) {
        project("empty", version) {
            val packageDependency = createPackageDependency()
            packageDependency.writePackageDependencySource("1")

            initProjectWithPackageDependency()

            build(fingerprintXcodebuildIphoneSimulatorTask) {
                assertTasksExecuted(fingerprintXcodebuildIphoneSimulatorTask)
            }
            val originalFingerprint = readIphonesimulatorFingerprint()

            build(fingerprintXcodebuildIphoneSimulatorTask) {
                assertTasksUpToDate(fingerprintXcodebuildIphoneSimulatorTask)
            }
            assertEquals(originalFingerprint, readIphonesimulatorFingerprint())

            packageDependency.writePackageDependencySource("2")

            build(fingerprintXcodebuildIphoneSimulatorTask) {
                assertTasksUpToDate(fingerprintXcodebuildIphoneSimulatorTask)
            }

            assertEquals(
                originalFingerprint,
                readIphonesimulatorFingerprint(),
                "Changing local Swift package source content should not invalidate the prepare fingerprint task"
            )
        }
    }

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    fun `adding local Swift package source file does not invalidate prepare xcodebuild dump fingerprint task`(version: GradleVersion) {
        project("empty", version) {
            val packageDependency = createPackageDependency()

            initProjectWithPackageDependency()

            build(fingerprintXcodebuildIphoneSimulatorTask) {
                assertTasksExecuted(fingerprintXcodebuildIphoneSimulatorTask)
            }
            val originalFingerprint = readIphonesimulatorFingerprint()

            packageDependency.resolve("Sources/PackageDependency/Extra.swift").writeText(
                """
                public struct Extra {
                    public static let value = "extra"
                }
                """.trimIndent()
            )

            build(fingerprintXcodebuildIphoneSimulatorTask) {
                assertTasksUpToDate(fingerprintXcodebuildIphoneSimulatorTask)
            }

            assertEquals(
                originalFingerprint,
                readIphonesimulatorFingerprint(),
                "Adding a local Swift package source file should not invalidate the prepare fingerprint task"
            )
        }
    }

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    fun `deleting local Swift package source file does not invalidate prepare xcodebuild dump fingerprint task`(version: GradleVersion) {
        project("empty", version) {
            val packageDependency = createPackageDependency()
            val extraSource = packageDependency.resolve("Sources/PackageDependency/Extra.swift").apply {
                writeText(
                    """
                    public struct Extra {
                        public static let value = "extra"
                    }
                    """.trimIndent()
                )
            }

            initProjectWithPackageDependency()

            build(fingerprintXcodebuildIphoneSimulatorTask) {
                assertTasksExecuted(fingerprintXcodebuildIphoneSimulatorTask)
            }
            val originalFingerprint = readIphonesimulatorFingerprint()

            extraSource.deleteExisting()

            build(fingerprintXcodebuildIphoneSimulatorTask) {
                assertTasksUpToDate(fingerprintXcodebuildIphoneSimulatorTask)
            }

            assertEquals(
                originalFingerprint,
                readIphonesimulatorFingerprint(),
                "Deleting a local Swift package source file should not invalidate the prepare fingerprint task"
            )
        }
    }

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    fun `changing SwiftPM build settings invalidates prepare xcodebuild dump fingerprint task`(version: GradleVersion) {
        val useAlternateDeploymentTarget = "useAlternateDeploymentTarget"
        val packageDependencyDirName = "PackageDependency"

        project("empty", version) {
            createPackageDependency()

            withLockFileFixture {
                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        iosMinimumDeploymentTarget.set(
                            if (project.providers.gradleProperty(useAlternateDeploymentTarget).isPresent) "16.0" else "15.0"
                        )

                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(packageDependencyDirName),
                            products = listOf("PackageDependency"),
                        )
                    }
                }
            }

            build(fingerprintXcodebuildIphoneSimulatorTask) {
                assertTasksExecuted(fingerprintXcodebuildIphoneSimulatorTask)
            }
            val originalFingerprint = readIphonesimulatorFingerprint()

            build(fingerprintXcodebuildIphoneSimulatorTask, "-P$useAlternateDeploymentTarget=true") {
                assertTasksExecuted(fingerprintXcodebuildIphoneSimulatorTask)
            }

            assertNotEquals(
                originalFingerprint,
                readIphonesimulatorFingerprint(),
                "Changing SwiftPM build settings should invalidate the prepare fingerprint task"
            )
        }
    }

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    fun `changing remote SwiftPM dependency version invalidates prepare xcodebuild dump fingerprint task`(version: GradleVersion) {
        val useAlternateVersion = "useAlternateVersion"
        val repoName = "RemotePackage"

        project("empty", version) {
            withLockFileFixture {
                val repo = repoRef(repoName).also { createRepo(it.name, listOf("1.0.0", "1.0.1")) }

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repo.url),
                            version = exact(
                                if (project.providers.gradleProperty(useAlternateVersion).isPresent) "1.0.1" else "1.0.0"
                            ),
                            products = listOf(product(repo.name)),
                        )
                    }
                }

                build(":convertSyntheticImportProjectIntoDefFileIphonesimulator") {
                    assertTasksExecuted(fingerprintXcodebuildIphoneSimulatorTask)
                }
                val originalXcodeBuildFingerprint = readIphonesimulatorFingerprint()
                val originalPackageFingerprint = localPackageFingerprint().readText()

                val lockFile =
                    projectPath.resolve("build/kotlin/swiftSyntheticPackages/${originalPackageFingerprint.split("\n")[1]}/Package.resolved")
                val originalLockFile = parsePackageResolved(lockFile.readText())
                assertEquals(listOf("1.0.0"), originalLockFile.pins.map { it.state.version })

                build(":convertSyntheticImportProjectIntoDefFileIphonesimulator", "-P$useAlternateVersion=true") {
                    assertTasksExecuted(fingerprintXcodebuildIphoneSimulatorTask)
                    assertTasksExecuted(":dumpXcodebuildArgsIphonesimulator")
                    assertTasksExecuted(":convertSyntheticImportProjectIntoDefFileIphonesimulator")
                }

                val finalLockFile = parsePackageResolved(lockFile.readText())
                assertEquals(listOf("1.0.1"), finalLockFile.pins.map { it.state.version })

                assertNotEquals(
                    originalXcodeBuildFingerprint.split("\n")[0],
                    readIphonesimulatorFingerprint().split("\n")[0],
                    "Changing a remote SwiftPM dependency version should invalidate the xcodebuild fingerprint task"
                )
                assertNotEquals(
                    originalPackageFingerprint.split("\n")[0],
                    localPackageFingerprint().readText().split("\n")[0],
                    "Changing a remote SwiftPM dependency version should invalidate the synthetic fingerprint task"
                )

                assertEquals(
                    originalXcodeBuildFingerprint.split("\n")[1],
                    readIphonesimulatorFingerprint().split("\n")[1],
                    "Changing a remote SwiftPM dependency version shouldn't invalidate version invariant part of the fingerprint"
                )
                assertEquals(
                    originalPackageFingerprint.split("\n")[1],
                    localPackageFingerprint().readText().split("\n")[1],
                    "Changing a remote SwiftPM dependency version shouldn't invalidate version invariant part of the fingerprint"
                )
            }
        }
    }

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    fun `changing selected SwiftPM product invalidates prepare xcodebuild dump fingerprint task`(version: GradleVersion) {
        val useAlternateProduct = "useAlternateProduct"
        val repoName = "RemotePackage"
        val productA = "ProductA"
        val productB = "ProductB"

        project("empty", version) {
            withLockFileFixture {
                val repo = repoRef(repoName).also {
                    createSwiftPmGitRepoWithTags(
                        reposRoot = reposRoot,
                        packageName = it.name,
                        tags = listOf("1.0.0"),
                        fileByTag = mapOf(
                            "1.0.0" to mapOf(
                                "Package.swift" to """
                                // swift-tools-version: 5.9
                                import PackageDescription

                                let package = Package(
                                    name: "$repoName",
                                    platforms: [.iOS(.v15)],
                                    products: [
                                        .library(name: "$productA", targets: ["$productA"]),
                                        .library(name: "$productB", targets: ["$productB"]),
                                    ],
                                    targets: [
                                        .target(name: "$productA"),
                                        .target(name: "$productB"),
                                    ]
                                )
                            """.trimIndent(),
                                "Sources/$productA/$productA.swift" to
                                        "public struct $productA { public static let value = \"$productA\" }\n",
                                "Sources/$productB/$productB.swift" to
                                        "public struct $productB { public static let value = \"$productB\" }\n",
                            )
                        )
                    )
                }

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repo.url),
                            version = exact("1.0.0"),
                            products = listOf(
                                product(
                                    if (project.providers.gradleProperty(useAlternateProduct).isPresent) productB else productA
                                )
                            ),
                        )
                    }
                }

                build(fingerprintXcodebuildIphoneSimulatorTask) {
                    assertTasksExecuted(fingerprintXcodebuildIphoneSimulatorTask)
                }
                val originalFingerprint = readIphonesimulatorFingerprint()

                build(fingerprintXcodebuildIphoneSimulatorTask, "-P$useAlternateProduct=true") {
                    assertTasksExecuted(fingerprintXcodebuildIphoneSimulatorTask)
                }

                assertNotEquals(
                    originalFingerprint,
                    readIphonesimulatorFingerprint(),
                    "Changing selected SwiftPM product should invalidate the xcodebuild fingerprint task"
                )
            }
        }
    }

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    fun `changing package resolved synchronization identifier invalidates prepare xcodebuild dump fingerprint task`(version: GradleVersion) {
        val useAlternateIdentifier = "useAlternateIdentifier"
        val packageDependencyDirName = "PackageDependency"

        project("empty", version) {
            createPackageDependency()

            withLockFileFixture {
                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(
                            if (project.providers.gradleProperty(useAlternateIdentifier).isPresent) "alternate" else "default"
                        )

                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(packageDependencyDirName),
                            products = listOf("PackageDependency"),
                        )
                    }
                }

                build(fingerprintXcodebuildIphoneSimulatorTask) {
                    assertTasksExecuted(fingerprintXcodebuildIphoneSimulatorTask)
                }
                val originalFingerprint = readIphonesimulatorFingerprint()

                build(fingerprintXcodebuildIphoneSimulatorTask, "-P$useAlternateIdentifier=true") {
                    assertTasksExecuted(fingerprintXcodebuildIphoneSimulatorTask)
                }

                assertNotEquals(
                    originalFingerprint,
                    readIphonesimulatorFingerprint(),
                    "Changing Package.resolved synchronization identifier should invalidate the prepare fingerprint task"
                )
            }
        }
    }

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `various different dependency versions with same lock identifier generate same fingerprints`(version: GradleVersion) {
        val useMapsDifferentVersions = "useMapsDifferentVersions"

        project("empty", version) {
            withLockFileFixture {
                val mapsRepo = repoRef("Maps").also { createRepo(it.name, listOf("1.0.0", "1.0.1")) }

                initSwiftPmProject(cacheDirFile) {
                    val version = if (project.hasProperty(useMapsDifferentVersions)) {
                        "1.0.1"
                    } else {
                        "1.0.0"
                    }

                    swiftPMDependencies {
                        swiftPackage(
                            url = url(mapsRepo.url),
                            version = exact(version),
                            products = listOf(product(mapsRepo.name))
                        )
                    }
                }

                val kmpMapsProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(mapsRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(mapsRepo.name))
                            )
                        }
                    }
                }
                include(kmpMapsProject, "kmpMapsProject")

                val prepareFingerPrintIphoneos = lowerCamelCaseName(
                    FingerprintXcodeBuild.TASK_NAME,
                    "iphonesimulator"
                )

                val kmpMapsXcodebuildFingerprint = localXcodebuildFingerprint("kmpMapsProject", "iphonesimulator")
                val rootXcodebuildFingerprint = localXcodebuildFingerprint(sdk = "iphonesimulator")

                build(
                    ":$prepareFingerPrintIphoneos",
                    ":kmpMapsProject:$prepareFingerPrintIphoneos",
                ) {

                    assertEquals(
                        parseSwiftPMFingerprint(kmpMapsXcodebuildFingerprint),
                        parseSwiftPMFingerprint(rootXcodebuildFingerprint),
                        "Projects with different dependencies and build settings should have different fingerprint"

                    )
                }

                build(
                    ":$prepareFingerPrintIphoneos", "-P${useMapsDifferentVersions}=true",
                    ":kmpMapsProject:$prepareFingerPrintIphoneos"
                ) {

                    assertEquals(
                        parseSwiftPMFingerprint(kmpMapsXcodebuildFingerprint),
                        parseSwiftPMFingerprint(rootXcodebuildFingerprint),
                        "Projects with different dependencies and build settings should have different fingerprint"
                    )
                }
            }
        }
    }

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `different dependency sets generate different fingerprints`(version: GradleVersion) {
        val useMapsRepo = "useMaps"

        project("empty", version) {
            withLockFileFixture {
                val mapsRepo = repoRef("Maps").also { createRepo(it.name, listOf("1.0.0")) }
                val crpytoRepo = repoRef("Crypto").also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {
                    if (project.hasProperty(useMapsRepo)) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(mapsRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(mapsRepo.name))
                            )
                        }
                    } else {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(crpytoRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(crpytoRepo.name))
                            )
                        }
                    }
                }

                val kmpMapsProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(mapsRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(mapsRepo.name))
                            )
                        }
                    }
                }
                include(kmpMapsProject, "kmpMapsProject")

                val prepareFingerPrintIphoneos = lowerCamelCaseName(
                    FingerprintXcodeBuild.TASK_NAME,
                    "iphonesimulator"
                )

                val kmpMapsXcodebuildFingerprint = localXcodebuildFingerprint("kmpMapsProject", "iphonesimulator")
                val rootXcodebuildFingerprint = localXcodebuildFingerprint(sdk = "iphonesimulator")

                build(
                    ":$prepareFingerPrintIphoneos",
                    ":kmpMapsProject:$prepareFingerPrintIphoneos",
                ) {

                    assertNotEquals(
                        parseSwiftPMFingerprint(kmpMapsXcodebuildFingerprint),
                        parseSwiftPMFingerprint(rootXcodebuildFingerprint),
                        "Projects with different dependencies and build settings should have different fingerprint"

                    )
                }

                build(
                    ":$prepareFingerPrintIphoneos", "-P${useMapsRepo}=true",
                    ":kmpMapsProject:$prepareFingerPrintIphoneos"
                ) {

                    assertEquals(
                        parseSwiftPMFingerprint(kmpMapsXcodebuildFingerprint),
                        parseSwiftPMFingerprint(rootXcodebuildFingerprint),
                        "Projects with same dependencies and build settings should have same fingerprint"

                    )
                }
            }
        }
    }

    private fun TestProject.createPackageDependency(): Path =
        projectPath.resolve("PackageDependency").also {
            createLocalSwiftPackage(
                localPackageDir = it,
                packageName = "PackageDependency",
                sourceLanguage = SwiftPackageSourceLanguage.SWIFT,
            )
        }

    private fun Path.writePackageDependencySource(version: String) {
        resolve("Sources/PackageDependency/PackageDependency.swift").writeText(
            """
            public struct PackageDependency {
                public static let value = "$version"
            }
            """.trimIndent()
        )
    }

    private fun TestProject.initProjectWithPackageDependency() {
        withLockFileFixture {
            initSwiftPmProject(cacheDirFile) {
                swiftPMDependencies {
                    localSwiftPackage(
                        directory = project.layout.projectDirectory.dir("PackageDependency"),
                        products = listOf("PackageDependency"),
                    )
                }
            }
        }
    }

    private fun TestProject.readIphonesimulatorFingerprint(): String =
        localIphonesimulatorDumpFingerprintFile().toFile().readText().trim()

}

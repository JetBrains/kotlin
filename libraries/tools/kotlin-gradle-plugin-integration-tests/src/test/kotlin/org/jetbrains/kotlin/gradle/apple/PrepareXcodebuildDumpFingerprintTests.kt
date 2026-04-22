/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PackageResolvedSynchronization
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PrepareXcodeBuildArgsDumpFingerprint
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
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class PrepareXcodebuildDumpFingerprintTests : KGPBaseTest() {

    private val prepareIphonesimulatorTask =
        ":${lowerCamelCaseName(PrepareXcodeBuildArgsDumpFingerprint.TASK_NAME, "iphonesimulator")}"

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    fun `local Swift package source content change invalidates prepare xcodebuild dump fingerprint task`(version: GradleVersion) {
        project("empty", version) {
            val packageDependency = createPackageDependency()
            packageDependency.writePackageDependencySource("1")

            initProjectWithPackageDependency()

            build(prepareIphonesimulatorTask) {
                assertTasksExecuted(prepareIphonesimulatorTask)
            }
            val originalFingerprint = readIphonesimulatorFingerprint()

            build(prepareIphonesimulatorTask) {
                assertTasksUpToDate(prepareIphonesimulatorTask)
            }
            assertEquals(originalFingerprint, readIphonesimulatorFingerprint())

            packageDependency.writePackageDependencySource("2")

            build(prepareIphonesimulatorTask) {
                assertTasksExecuted(prepareIphonesimulatorTask)
            }

            assertNotEquals(
                originalFingerprint,
                readIphonesimulatorFingerprint(),
                "Changing local Swift package source content should invalidate the prepare fingerprint task"
            )
        }
    }

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    fun `adding local Swift package source file invalidates prepare xcodebuild dump fingerprint task`(version: GradleVersion) {
        project("empty", version) {
            val packageDependency = createPackageDependency()

            initProjectWithPackageDependency()

            build(prepareIphonesimulatorTask) {
                assertTasksExecuted(prepareIphonesimulatorTask)
            }
            val originalFingerprint = readIphonesimulatorFingerprint()

            packageDependency.resolve("Sources/PackageDependency/Extra.swift").writeText(
                """
                public struct Extra {
                    public static let value = "extra"
                }
                """.trimIndent()
            )

            build(prepareIphonesimulatorTask) {
                assertTasksExecuted(prepareIphonesimulatorTask)
            }

            assertNotEquals(
                originalFingerprint,
                readIphonesimulatorFingerprint(),
                "Adding a local Swift package source file should invalidate the prepare fingerprint task"
            )
        }
    }

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    fun `deleting local Swift package source file invalidates prepare xcodebuild dump fingerprint task`(version: GradleVersion) {
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

            build(prepareIphonesimulatorTask) {
                assertTasksExecuted(prepareIphonesimulatorTask)
            }
            val originalFingerprint = readIphonesimulatorFingerprint()

            extraSource.deleteExisting()

            build(prepareIphonesimulatorTask) {
                assertTasksExecuted(prepareIphonesimulatorTask)
            }

            assertNotEquals(
                originalFingerprint,
                readIphonesimulatorFingerprint(),
                "Deleting a local Swift package source file should invalidate the prepare fingerprint task"
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

            build(prepareIphonesimulatorTask) {
                assertTasksExecuted(prepareIphonesimulatorTask)
            }
            val originalFingerprint = readIphonesimulatorFingerprint()

            build(prepareIphonesimulatorTask, "-P$useAlternateDeploymentTarget=true") {
                assertTasksExecuted(prepareIphonesimulatorTask)
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

                build(prepareIphonesimulatorTask) {
                    assertTasksExecuted(prepareIphonesimulatorTask)
                }
                val originalFingerprint = readIphonesimulatorFingerprint()

                build(prepareIphonesimulatorTask, "-P$useAlternateVersion=true") {
                    assertTasksExecuted(prepareIphonesimulatorTask)
                }

                assertNotEquals(
                    originalFingerprint,
                    readIphonesimulatorFingerprint(),
                    "Changing a remote SwiftPM dependency version should invalidate the prepare fingerprint task"
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

                build(prepareIphonesimulatorTask) {
                    assertTasksExecuted(prepareIphonesimulatorTask)
                }
                val originalFingerprint = readIphonesimulatorFingerprint()

                build(prepareIphonesimulatorTask, "-P$useAlternateProduct=true") {
                    assertTasksExecuted(prepareIphonesimulatorTask)
                }

                assertNotEquals(
                    originalFingerprint,
                    readIphonesimulatorFingerprint(),
                    "Changing selected SwiftPM product should invalidate the prepare fingerprint task"
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

                build(prepareIphonesimulatorTask) {
                    assertTasksExecuted(prepareIphonesimulatorTask)
                }
                val originalFingerprint = readIphonesimulatorFingerprint()

                build(prepareIphonesimulatorTask, "-P$useAlternateIdentifier=true") {
                    assertTasksExecuted(prepareIphonesimulatorTask)
                }

                assertNotEquals(
                    originalFingerprint,
                    readIphonesimulatorFingerprint(),
                    "Changing Package.resolved synchronization identifier should invalidate the prepare fingerprint task"
                )
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
        parseSwiftPMXcodeBuildExecutionHash(
            localIphonesimulatorDumpFingerprintFile()
        )
}

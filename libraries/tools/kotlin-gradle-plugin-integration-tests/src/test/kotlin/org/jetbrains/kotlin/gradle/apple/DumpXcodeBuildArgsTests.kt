/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PackageResolvedSynchronization
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.assertOutputContainsExactlyTimes
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.findTasksByPattern
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.gradle.uklibs.include
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.createDirectories
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class DumpXcodeBuildArgsTests : KGPBaseTest() {

    @GradleTest
    fun `smoke test - xcodebuild args are dumped into task output directory`(version: GradleVersion) {
        project("empty", version) {
            withLockFileFixture {
                val packageOne = projectPath.resolve("packageOne").also { it.createDirectories() }.toFile()
                runProcess(listOf("swift", "package", "init", "--type", "library"), packageOne)
                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir("packageOne"),
                            products = listOf("packageOne"),
                        )
                    }
                }
                build("dumpXcodebuildArgsIphoneos")

                val dumpDir = localDumpDir(sdk = "iphoneos")
                assertDumpDirectoryContainsXcodebuildArgsDump(dumpDir)
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `same fingerprint across projects reuses one shared dump execution`(version: GradleVersion) {
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val repoName = "SharedPackage"

        project("empty", version) {
            withLockFileFixture {
                val sharedRepo = repoRef(repoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {}

                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(sharedRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(sharedRepo.name)),
                            )
                        }
                    }
                }
                val buzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(sharedRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(sharedRepo.name)),
                            )
                        }
                    }
                }

                include(fuzzProject, fuzzProjectName)
                include(buzzProject, buzzProjectName)

                build(
                    ":$fuzzProjectName:convertSyntheticImportProjectIntoDefFileIphoneos",
                    ":$buzzProjectName:convertSyntheticImportProjectIntoDefFileIphoneos",
                ) {
                    val dumpTasks = findTasksByPattern(Regex(":(${fuzzProjectName}|${buzzProjectName}):dumpXcodebuildArgsIphoneos"))

                    assertEquals(2, dumpTasks.size)
                    assertTasksExecuted(dumpTasks)
                    assertOutputContainsExactlyTimes("Starting process 'command 'xcodebuild''", 1)
                    assertSharedDumpDirsHaveSameFiles(
                        localIphoneosDumpDir(fuzzProjectName),
                        localIphoneosDumpDir(buzzProjectName),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `same fingerprint across SDKs uses one root bucket with separate xcodebuild executions`(version: GradleVersion) {
        val appProjectName = "app"
        val repoName = "SharedPackage"

        project("empty", version) {
            withLockFileFixture {
                val sharedRepo = repoRef(repoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {}

                val appProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(sharedRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(sharedRepo.name)),
                            )
                        }
                    }
                }

                include(appProject, appProjectName)

                build(
                    ":$appProjectName:convertSyntheticImportProjectIntoDefFileIphoneos",
                    ":$appProjectName:convertSyntheticImportProjectIntoDefFileIphonesimulator",
                ) {
                    val dumpTasks = findTasksByPattern(
                        Regex(":$appProjectName:dumpXcodebuildArgs(Iphoneos|Iphonesimulator)")
                    )

                    assertEquals(2, dumpTasks.size)
                    assertTasksExecuted(dumpTasks)
                    assertOutputContainsExactlyTimes("Starting process 'command 'xcodebuild''", 2)

                    val iphoneosDumpLocation = localIphoneosDumpDir(appProjectName)
                    val iphonesimulatorDumpLocation = localIphonesimulatorDumpDir(appProjectName)
                    assertNotEquals(
                        iphoneosDumpLocation,
                        iphonesimulatorDumpLocation,
                        "Different SDKs should write separate clang/linker dump directories"
                    )
                    assertEquals(
                        sharedRootBucketDir(iphoneosDumpLocation),
                        sharedRootBucketDir(iphonesimulatorDumpLocation),
                        "Different SDKs for the same SwiftPM graph should share the same root-build bucket"
                    )
                    assertEquals(
                        parseSwiftPMFingerprint(localXcodebuildFingerprint(appProjectName, "iphoneos")),
                        parseSwiftPMFingerprint(localXcodebuildFingerprint(appProjectName, "iphonesimulator")),
                        "The xcodebuild execution fingerprint should be SDK-independent"
                    )
                    assertDumpDirectoryContainsXcodebuildArgsDump(iphoneosDumpLocation)
                    assertDumpDirectoryContainsXcodebuildArgsDump(iphonesimulatorDumpLocation)
                    assertLocalDerivedDataDirsExist(
                        localIphoneosDerivedDataDir(appProjectName),
                        localIphonesimulatorDerivedDataDir(appProjectName),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `different identifiers with same declared dependencies execute separate xcodebuild dumps`(version: GradleVersion) {
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val repoName = "SharedPackage"

        project("empty", version) {
            withLockFileFixture {
                val sharedRepo = repoRef(repoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {}

                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier("fuzzLock")
                            swiftPackage(
                                url = url(sharedRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(sharedRepo.name)),
                            )
                        }
                    }
                }
                val buzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier("buzzLock")
                            swiftPackage(
                                url = url(sharedRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(sharedRepo.name)),
                            )
                        }
                    }
                }

                include(fuzzProject, fuzzProjectName)
                include(buzzProject, buzzProjectName)

                build(
                    ":$fuzzProjectName:dumpXcodebuildArgsIphoneos",
                    ":$buzzProjectName:dumpXcodebuildArgsIphoneos",
                ) {
                    val dumpTasks = findTasksByPattern(Regex(":(${fuzzProjectName}|${buzzProjectName}):dumpXcodebuildArgsIphoneos"))

                    assertEquals(2, dumpTasks.size, "Different identifiers should still keep one local dump task per project")
                    assertTasksExecuted(dumpTasks)
                    assertOutputContainsExactlyTimes("Starting process 'command 'xcodebuild''", 2)
                    assertDumpDirectoryContainsXcodebuildArgsDump(localIphoneosDumpDir(fuzzProjectName))
                    assertDumpDirectoryContainsXcodebuildArgsDump(localIphoneosDumpDir(buzzProjectName))

                    val fuzzFingerprintFile = localIphoneosDumpFingerprintFile(fuzzProjectName)
                    val buzzFingerprintFile = localIphoneosDumpFingerprintFile(buzzProjectName)
                    assertFileExists(fuzzFingerprintFile)
                    assertFileExists(buzzFingerprintFile)
                    assertNotEquals(
                        parseSwiftPMFingerprint(fuzzFingerprintFile),
                        parseSwiftPMFingerprint(buzzFingerprintFile),
                        "The xcodebuild execution hash should include lock identifiers"
                    )
                    assertNotEquals(
                        sharedRootBucketDir(localIphoneosDumpDir(fuzzProjectName)),
                        sharedRootBucketDir(localIphoneosDumpDir(buzzProjectName)),
                        "Different lock identifiers should use separate root-build buckets"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `different selected products execute separate xcodebuild dumps`(version: GradleVersion) {
        val coreProjectName = "core"
        val logsProjectName = "logs"
        val repoName = "SharedPackage"
        val productA = "ProductA"
        val productB = "ProductB"

        project("empty", version) {
            withLockFileFixture {
                val sharedRepo = repoRef(repoName).also {
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

                initSwiftPmProject(cacheDirFile) {}

                val coreProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier("coreLock")
                            swiftPackage(
                                url = url(sharedRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(productA)),
                            )
                        }
                    }
                }
                val logsProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier("logsLock")
                            swiftPackage(
                                url = url(sharedRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(productB)),
                            )
                        }
                    }
                }

                include(coreProject, coreProjectName)
                include(logsProject, logsProjectName)

                build(
                    ":$coreProjectName:dumpXcodebuildArgsIphoneos",
                    ":$logsProjectName:dumpXcodebuildArgsIphoneos",
                ) {
                    val dumpTasks = findTasksByPattern(Regex(":(${coreProjectName}|${logsProjectName}):dumpXcodebuildArgsIphoneos"))

                    assertEquals(2, dumpTasks.size)
                    assertTasksExecuted(dumpTasks)
                    assertOutputContainsExactlyTimes("Starting process 'command 'xcodebuild''", 2)

                    val coreFingerprintFile = localIphoneosDumpFingerprintFile(coreProjectName)
                    val logsFingerprintFile = localIphoneosDumpFingerprintFile(logsProjectName)
                    assertNotEquals(
                        parseSwiftPMFingerprint(coreFingerprintFile),
                        parseSwiftPMFingerprint(logsFingerprintFile),
                        "Different selected products should not share one xcodebuild dump"
                    )
                    assertNotEquals(
                        sharedRootBucketDir(localIphoneosDumpDir(coreProjectName)),
                        sharedRootBucketDir(localIphoneosDumpDir(logsProjectName)),
                        "Different xcodebuild execution hashes should use separate root-build buckets"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `different dependencies execute separate xcodebuild dumps`(version: GradleVersion) {
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val fuzzRepoName = "FuzzPackage"
        val buzzRepoName = "BuzzPackage"

        project("empty", version) {
            withLockFileFixture {
                val fuzzRepo = repoRef(fuzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }
                val buzzRepo = repoRef(buzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {}

                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier("fuzzLock")
                            swiftPackage(
                                url = url(fuzzRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(fuzzRepo.name)),
                            )
                        }
                    }
                }
                val buzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier("buzzLock")
                            swiftPackage(
                                url = url(buzzRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(buzzRepo.name)),
                            )
                        }
                    }
                }

                include(fuzzProject, fuzzProjectName)
                include(buzzProject, buzzProjectName)

                build(
                    ":$fuzzProjectName:dumpXcodebuildArgsIphoneos",
                    ":$buzzProjectName:dumpXcodebuildArgsIphoneos",
                ) {
                    val dumpTasks = findTasksByPattern(Regex(":(${fuzzProjectName}|${buzzProjectName}):dumpXcodebuildArgsIphoneos"))

                    assertEquals(2, dumpTasks.size, "Different dependency graphs should still keep one local dump task per project")
                    assertTasksExecuted(dumpTasks)
                    assertOutputContainsExactlyTimes("Starting process 'command 'xcodebuild''", 2)
                    assertDumpDirectoryContainsXcodebuildArgsDump(localIphoneosDumpDir(fuzzProjectName))
                    assertDumpDirectoryContainsXcodebuildArgsDump(localIphoneosDumpDir(buzzProjectName))
                }
            }
        }
    }
}

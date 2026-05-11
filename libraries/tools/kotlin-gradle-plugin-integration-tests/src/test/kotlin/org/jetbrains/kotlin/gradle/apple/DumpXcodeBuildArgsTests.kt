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
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.assertDirectoryExists
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.assertOutputContainsExactlyTimes
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.findTasksByPattern
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.gradle.uklibs.include
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class DumpXcodeBuildArgsTests : KGPBaseTest() {

    internal fun LockFileTestFixture.includeKmpMapsConsumerProjects(
        version: GradleVersion,
        mapsProjectName: String,
        leftProjectName: String,
        rightProjectName: String,
        repoName: String,
        leftPackageResolvedIdentifier: String = "default",
        rightPackageResolvedIdentifier: String = "default",
    ) {
        val sharedRepo = repoRef(repoName).also { createRepo(it.name, listOf("1.0.0")) }

        project.initSwiftPmProject(cacheDirFile) {}

        val mapsProject = project("empty", version) {
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

        val leftProject = project("empty", version) {
            initSwiftPmProject(cacheDirFile) {
                swiftPMDependencies {
                    packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(leftPackageResolvedIdentifier)
                }
                sourceSets.getByName("iosArm64Main").dependencies {
                    implementation(project(":$mapsProjectName"))
                }
            }
        }

        val rightProject = project("empty", version) {
            initSwiftPmProject(cacheDirFile) {
                swiftPMDependencies {
                    packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(rightPackageResolvedIdentifier)
                }
                sourceSets.getByName("iosArm64Main").dependencies {
                    implementation(project(":$mapsProjectName"))
                }
            }
        }

        project.include(mapsProject, mapsProjectName)
        project.include(leftProject, leftProjectName)
        project.include(rightProject, rightProjectName)
    }

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

                val dumpDir = localDumpDir( sdk = "iphoneos")
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
                    assertOutputContainsExactlyTimes("Command line invocation:", 1)
                    assertSharedDumpDirsHaveSameFiles(
                        localIphoneosDumpDir(fuzzProjectName),
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
                    assertOutputContainsExactlyTimes("Command line invocation:", 2)

                    val iphoneosDumpLocation = localDumpLocation(appProjectName, "iphoneos")
                    val iphonesimulatorDumpLocation = localDumpLocation(appProjectName, "iphonesimulator")
                    assertNotEquals(
                        iphoneosDumpLocation.dumpedXcodeBuildArgsDir,
                        iphonesimulatorDumpLocation.dumpedXcodeBuildArgsDir,
                        "Different SDKs should write separate clang/linker dump directories"
                    )
                    assertEquals(
                        sharedRootBucketDir(iphoneosDumpLocation.dumpedXcodeBuildArgsDir),
                        sharedRootBucketDir(iphonesimulatorDumpLocation.dumpedXcodeBuildArgsDir),
                        "Different SDKs for the same SwiftPM graph should share the same root-build bucket"
                    )
                    assertEquals(
                        xcodebuildExecutionHash(localXcodebuildExecutionHashFile(appProjectName, "iphoneos")),
                        xcodebuildExecutionHash(localXcodebuildExecutionHashFile(appProjectName, "iphonesimulator")),
                        "The xcodebuild execution fingerprint should be SDK-independent"
                    )
                    assertDumpDirectoryContainsXcodebuildArgsDump(iphoneosDumpLocation.dumpedXcodeBuildArgsDir)
                    assertDumpDirectoryContainsXcodebuildArgsDump(iphonesimulatorDumpLocation.dumpedXcodeBuildArgsDir)
                    assertLocalDerivedDataDirsExist(
                        iphoneosDumpLocation.derivedDataDir,
                        iphonesimulatorDumpLocation.derivedDataDir,
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
                    assertOutputContainsExactlyTimes("Command line invocation:", 2)
                    assertDumpDirectoryContainsXcodebuildArgsDump(localIphoneosDumpDir(fuzzProjectName))
                    assertDumpDirectoryContainsXcodebuildArgsDump(localIphoneosDumpDir(buzzProjectName))

                    val fuzzFingerprintFile = localIphoneosDumpFingerprintFile(fuzzProjectName)
                    val buzzFingerprintFile = localIphoneosDumpFingerprintFile(buzzProjectName)
                    assertFileExists(fuzzFingerprintFile)
                    assertFileExists(buzzFingerprintFile)
                    assertNotEquals(
                        xcodebuildExecutionHash(fuzzFingerprintFile),
                        xcodebuildExecutionHash(buzzFingerprintFile),
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
                    assertOutputContainsExactlyTimes("Command line invocation:", 2)

                    val coreFingerprintFile = localIphoneosDumpFingerprintFile(coreProjectName)
                    val logsFingerprintFile = localIphoneosDumpFingerprintFile(logsProjectName)
                    assertNotEquals(
                        xcodebuildExecutionHash(coreFingerprintFile),
                        xcodebuildExecutionHash(logsFingerprintFile),
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
                    assertOutputContainsExactlyTimes("Command line invocation:", 2)
                    assertDumpDirectoryContainsXcodebuildArgsDump(localIphoneosDumpDir(fuzzProjectName))
                    assertDumpDirectoryContainsXcodebuildArgsDump(localIphoneosDumpDir(buzzProjectName))
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `different identifiers with different resolved dependencies execute separate xcodebuild dumps`(version: GradleVersion) {
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val repoName = "SharedPackage"

        project("empty", version) {
            withLockFileFixture {
                val sharedRepo = repoRef(repoName).also { createRepo(it.name, listOf("1.0.0", "1.0.1")) }

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
                                version = exact("1.0.1"),
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
                    assertOutputContainsExactlyTimes("Command line invocation:", 2)
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `kmp maps consumers with different identifiers execute separate xcodebuild dumps`(version: GradleVersion) {
        val mapsProjectName = "kmp-maps"
        val leftProjectName = "left"
        val rightProjectName = "right"
        val repoName = "GoogleMapsLike"

        project("empty", version) {
            withLockFileFixture {
                includeKmpMapsConsumerProjects(
                    version = version,
                    mapsProjectName = mapsProjectName,
                    leftProjectName = leftProjectName,
                    rightProjectName = rightProjectName,
                    repoName = repoName,
                    leftPackageResolvedIdentifier = "leftLock",
                    rightPackageResolvedIdentifier = "rightLock",
                )

                build(
                    ":$leftProjectName:dumpXcodebuildArgsIphoneos",
                    ":$rightProjectName:dumpXcodebuildArgsIphoneos",
                ) {
                    val dumpTasks = findTasksByPattern(
                        Regex(":(${mapsProjectName}|${leftProjectName}|${rightProjectName}):dumpXcodebuildArgsIphoneos")
                    )

                    assertEquals(2, dumpTasks.size, "Both consumers should keep their own local dump task")
                    assertTasksExecuted(dumpTasks)
                    assertOutputContainsExactlyTimes("Command line invocation:", 2)
                    assertNotEquals(
                        sharedRootBucketDir(localIphoneosDumpDir(leftProjectName)),
                        sharedRootBucketDir(localIphoneosDumpDir(rightProjectName)),
                        "Different lock identifiers should use separate root-build buckets"
                    )
                    assertLocalDerivedDataDirsExist(
                        localIphoneosDerivedDataDir(leftProjectName),
                        localIphoneosDerivedDataDir(rightProjectName),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `fingerprint changes reflect on the dump task  configuration`(version: GradleVersion) {
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val repoName = "SharedPackage"
        val sharedIdentifier = "sharedLock"

        val useSameFingerprintVariable = "useSameFingerprint"

        project("empty", version) {
            withLockFileFixture {
                val sharedRepo = repoRef(repoName).also { createRepo(it.name, listOf("1.0.0", "1.0.1", "1.0.2")) }

                initSwiftPmProject(cacheDirFile) {}

                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        if (project.providers.gradleProperty("useSameFingerprint").isPresent) {
                            swiftPMDependencies {
                                packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(sharedIdentifier)
                                swiftPackage(
                                    url = url(sharedRepo.url),
                                    version = exact("1.0.0"),
                                    products = listOf(product(sharedRepo.name)),
                                )
                            }
                        } else {
                            swiftPMDependencies {
                                packageResolvedSynchronization = PackageResolvedSynchronization.Identifier("fuzzLock")
                                swiftPackage(
                                    url = url(sharedRepo.url),
                                    version = exact("1.0.1"),
                                    products = listOf(product(sharedRepo.name)),
                                )
                            }
                        }
                    }
                }

                val buzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        if (project.providers.gradleProperty("useSameFingerprint").isPresent) {
                            swiftPMDependencies {
                                packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(sharedIdentifier)
                                swiftPackage(
                                    url = url(sharedRepo.url),
                                    version = exact("1.0.0"),
                                    products = listOf(product(sharedRepo.name)),
                                )
                            }
                        } else {
                            swiftPMDependencies {
                                packageResolvedSynchronization = PackageResolvedSynchronization.Identifier("buzzLock")
                                swiftPackage(
                                    url = url(sharedRepo.url),
                                    version = exact("1.0.2"),
                                    products = listOf(product(sharedRepo.name)),
                                )
                            }
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

                    assertEquals(2, dumpTasks.size, "Using the different fingerprint task should produce two dump tasks")
                    assertTasksExecuted(dumpTasks)
                    assertOutputContainsExactlyTimes("Command line invocation:", 2)
                }

                build(
                    ":$fuzzProjectName:convertSyntheticImportProjectIntoDefFileIphoneos", "-P${useSameFingerprintVariable}=true",
                    ":$buzzProjectName:convertSyntheticImportProjectIntoDefFileIphoneos", "-P${useSameFingerprintVariable}=true",
                ) {
                    val dumpTasks = findTasksByPattern(Regex(":(${fuzzProjectName}|${buzzProjectName}):dumpXcodebuildArgsIphoneos"))

                    assertEquals(2, dumpTasks.size, "Using the same fingerprint still keeps one local dump task per project")
                    assertTasksExecuted(dumpTasks)
                    assertOutputContainsExactlyTimes("Command line invocation:", 1)
                    assertEquals(
                        materializedDumpFilesByRelativePath(localIphoneosDumpDir(fuzzProjectName)).keys,
                        materializedDumpFilesByRelativePath(localIphoneosDumpDir(buzzProjectName)).keys,
                        "Changing to one shared fingerprint should make both tasks point to the same xcodebuild dump files"
                    )
                    assertLocalDerivedDataDirsExist(
                        localIphoneosDerivedDataDir(fuzzProjectName),
                        localIphoneosDerivedDataDir(buzzProjectName),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `consumer reuses old matching bucket after owner dependency fingerprint changes`(version: GradleVersion) {
        val ownerProjectName = "owner"
        val consumerProjectName = "consumer"
        val sharedRepoName = "SharedPackage"
        val ownerOnlyRepoName = "OwnerPackage"
        val alternateOwnerPackage = "useAlternateOwnerPackage"

        project("empty", version) {
            withLockFileFixture {
                val sharedRepo = repoRef(sharedRepoName).also { createRepo(it.name, listOf("1.0.0")) }
                val ownerOnlyRepo = repoRef(ownerOnlyRepoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {}

                val ownerProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            val ownerRepo =
                                if (project.providers.gradleProperty(alternateOwnerPackage).isPresent) {
                                    ownerOnlyRepo
                                } else {
                                    sharedRepo
                                }

                            swiftPackage(
                                url = url(ownerRepo.url),
                                version = exact("1.0.0"),
                                products = listOf(product(ownerRepo.name)),
                            )
                        }
                    }
                }

                val consumerProject = project("empty", version) {
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

                include(ownerProject, ownerProjectName)
                include(consumerProject, consumerProjectName)

                build(":$ownerProjectName:dumpXcodebuildArgsIphoneos") {
                    assertOutputContainsExactlyTimes("Command line invocation:", 1)
                }

                val originalOwnerDumpDir = localIphoneosDumpDir(ownerProjectName)
                assertDumpDirectoryContainsXcodebuildArgsDump(originalOwnerDumpDir)

                build(
                    ":$ownerProjectName:dumpXcodebuildArgsIphoneos",
                    "-P$alternateOwnerPackage=true",
                ) {
                    assertOutputContainsExactlyTimes("Command line invocation:", 1)
                }

                val changedOwnerDumpDir = localIphoneosDumpDir(ownerProjectName)
                assertNotEquals(
                    originalOwnerDumpDir,
                    changedOwnerDumpDir,
                    "Changing the owner's dependency fingerprint should move it to a different root-build bucket"
                )

                build(":$consumerProjectName:dumpXcodebuildArgsIphoneos") {
                    assertOutputContainsExactlyTimes("Command line invocation:", 0)
                    assertEquals(
                        originalOwnerDumpDir,
                        localIphoneosDumpDir(consumerProjectName),
                        "The consumer should reuse the old still-valid bucket because its fingerprint matches the owner's original dependency graph"
                    )
                    assertDumpDirectoryContainsXcodebuildArgsDump(originalOwnerDumpDir)
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `mismatched root bucket stamp makes current target claim and rewrite bucket`(version: GradleVersion) {
        val ownerProjectName = "owner"
        val consumerProjectName = "consumer"
        val repoName = "SharedPackage"

        project("empty", version) {
            withLockFileFixture {
                val sharedRepo = repoRef(repoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {}

                val ownerProject = project("empty", version) {
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
                val consumerProject = project("empty", version) {
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

                include(ownerProject, ownerProjectName)
                include(consumerProject, consumerProjectName)

                build(":$ownerProjectName:dumpXcodebuildArgsIphoneos") {
                    assertOutputContainsExactlyTimes("Command line invocation:", 1)
                }
                val sharedDumpDir = localIphoneosDumpDir(ownerProjectName)
                writeMismatchedXcodeDumpFingerprintStamp(sharedDumpDir)

                build(":$consumerProjectName:dumpXcodebuildArgsIphoneos") {
                    assertOutputContainsExactlyTimes("Command line invocation:", 1)
                    assertEquals(
                        sharedDumpDir,
                        localIphoneosDumpDir(consumerProjectName),
                        "The consumer should claim the deterministic bucket for the matching xcodebuild execution fingerprint"
                    )
                    assertEquals(
                        xcodebuildExecutionHash(localIphoneosDumpFingerprintFile(consumerProjectName)),
                        fingerprintValue(xcodeDumpFingerprintStamp(sharedDumpDir), "xcodebuildExecutionHash"),
                        "The rewritten bucket stamp should match the consumer's xcodebuild execution fingerprint"
                    )
                }
            }
        }
    }
}

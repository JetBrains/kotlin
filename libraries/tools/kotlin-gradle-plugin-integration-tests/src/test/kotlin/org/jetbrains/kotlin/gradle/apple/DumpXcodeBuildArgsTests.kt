/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.register
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.createKotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.applePlatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.sdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.DumpXcodeBuildArgs
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.SyntheticProductType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PackageResolvedSynchronization
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMXcodeDumpBuildService
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.TransitiveSwiftPMDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.assertDirectoryExists
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.findTasksByPattern
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class DumpXcodeBuildArgsTests : KGPBaseTest() {

    private val xcodeDumpBucketName = Regex("[0-9a-f]{64}")
    private val materializedDumpEntries = listOf("clangDump.sh", "ldDump.sh", "clang_args_dump", "ld_args_dump")

    private fun TestProject.sharedDumpFingerprintDirs(vararg projectNames: String) =
        projectNames.flatMap { projectName ->
            projectPath.resolve("$projectName/build/kotlin/swiftImportClangDump/iphoneos/.buckets").toFile()
                .listFiles()
                .orEmpty()
                .filter { it.isDirectory && it.name.matches(xcodeDumpBucketName) }
        }

    private fun TestProject.localIphoneosDumpDir(projectName: String) =
        projectPath.resolve("$projectName/build/kotlin/swiftImportClangDump/iphoneos")

    private fun assertDumpDirectoryContainsXcodebuildArgsDump(dumpDir: java.nio.file.Path) {
        assertDirectoryExists(dumpDir)
        assertFileExists(dumpDir.resolve("clangDump.sh"))
        assertFileExists(dumpDir.resolve("ldDump.sh"))
        assertDirectoryExists(dumpDir.resolve("clang_args_dump"))
        assertDirectoryExists(dumpDir.resolve("ld_args_dump"))
    }

    private fun assertLocalDumpDirsCopiedFromSharedBucket(
        sharedDumpDir: File,
        vararg localDumpDirs: Path,
    ) {
        val sharedDumpFiles = materializedDumpFilesByRelativePath(sharedDumpDir)
        assertTrue(sharedDumpFiles.isNotEmpty(), "Shared dump bucket should contain xcodebuild dump files")

        localDumpDirs.forEach { localDumpDir ->
            assertDumpDirectoryContainsXcodebuildArgsDump(localDumpDir)
            assertEquals(
                sharedDumpFiles,
                materializedDumpFilesByRelativePath(localDumpDir.toFile()),
                "Local dump directory should be a copy of the shared dump bucket"
            )
        }
    }

    private fun materializedDumpFilesByRelativePath(dumpDir: File): Map<String, String> =
        materializedDumpEntries.asSequence()
            .map { dumpDir.resolve(it) }
            .filter { it.exists() }
            .flatMap { it.walkTopDown().asSequence() }
            .filter { it.isFile }
            .associate { file ->
                file.relativeTo(dumpDir).invariantSeparatorsPath to file.readText()
            }

    private fun LockFileTestFixture.includeKmpMapsConsumerProjects(
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
            val stubTrackedFiles = projectPath.resolve("trackedFilesStub").also { it.createFile() }.toFile()
            val packageOne = projectPath.resolve("packageOne").also { it.createDirectories() }.toFile()
            val packageResolved = projectPath.resolve("Package.resolved").also { it.createFile() }.toFile()
            runProcess(listOf("swift", "package", "init", "--type", "library"), packageOne)

            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.createKotlinExtension(KotlinMultiplatformExtension::class)
                val extension = project.locateOrRegisterSwiftPMDependenciesExtension().apply {
                    localSwiftPackage(
                        directory = project.layout.projectDirectory.dir("packageOne"),
                        products = listOf("packageOne"),
                    )
                }
                val packageGeneration = project.tasks.register<GenerateSyntheticLinkageImportProject>("packageGeneration") {
                    configureWithExtension(extension)
                    konanTargets.set(setOf(KonanTarget.IOS_SIMULATOR_ARM64))
                    dependencyIdentifierToImportedSwiftPMDependencies.set(TransitiveSwiftPMDependencies(emptyMap()))
                    syntheticProductType.set(SyntheticProductType.DYNAMIC)
                }

                project.tasks.register<DumpXcodeBuildArgs>("packageDumpArgs") {
                    dependsOn(packageGeneration)
                    xcodebuildPlatform.set(KonanTarget.IOS_SIMULATOR_ARM64.applePlatform)
                    xcodebuildSdk.set(KonanTarget.IOS_SIMULATOR_ARM64.appleTarget.sdk)
                    architectures.add(KonanTarget.IOS_SIMULATOR_ARM64.appleArchitecture)
                    hasSwiftPMDependencies.set(true)
                    packageResolvedFile.set(packageResolved)
                    packageResolvedSynchronization.set("identifier:default")
                    directSwiftPMDependencies.set(extension.swiftPMDependencies)
                    transitiveSwiftPMDependencies.set(TransitiveSwiftPMDependencies(emptyMap()))
                    filesToTrackFromLocalPackages.set(stubTrackedFiles)
                    syntheticImportProjectRoot.set(packageGeneration.map { it.syntheticImportProjectRoot.get() })
                    syntheticImportDd.set(project.layout.buildDirectory.dir("kotlin/customSwiftImportDd"))
                    coordinationService.set(
                        SwiftPMXcodeDumpBuildService.registerIfAbsent(project)
                    )
                    swiftPMDependenciesCheckout.set(project.layout.buildDirectory.dir("checkout"))
                    dumpedXcodeBuildArgsDir.set(
                        project.layout.buildDirectory.dir("kotlin/customSwiftImportDump/iphonesimulator")
                    )
                }
            }

            build("packageDumpArgs")

            val dumpDir = projectPath.resolve("build/kotlin/customSwiftImportDump/iphonesimulator")
            assertDumpDirectoryContainsXcodebuildArgsDump(dumpDir)
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
                    val sharedDumpDir = sharedDumpFingerprintDirs(fuzzProjectName, buzzProjectName).single()
                    assertLocalDumpDirsCopiedFromSharedBucket(
                        sharedDumpDir,
                        localIphoneosDumpDir(fuzzProjectName),
                        localIphoneosDumpDir(buzzProjectName),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `different identifiers with same resolved dependencies reuse one shared dump bucket`(version: GradleVersion) {
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
                    val sharedDumpDir = sharedDumpFingerprintDirs(fuzzProjectName, buzzProjectName).single()

                    assertEquals(2, dumpTasks.size, "Different identifiers should still keep one local dump task per project")
                    assertTasksExecuted(dumpTasks)
                    assertLocalDumpDirsCopiedFromSharedBucket(
                        sharedDumpDir,
                        localIphoneosDumpDir(fuzzProjectName),
                        localIphoneosDumpDir(buzzProjectName),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `different dependencies materialize separate shared dump buckets`(version: GradleVersion) {
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
                    assertEquals(
                        2,
                        sharedDumpFingerprintDirs(fuzzProjectName, buzzProjectName).size,
                        "Different dependency graphs should materialize separate shared buckets"
                    )
                    assertDumpDirectoryContainsXcodebuildArgsDump(localIphoneosDumpDir(fuzzProjectName))
                    assertDumpDirectoryContainsXcodebuildArgsDump(localIphoneosDumpDir(buzzProjectName))
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `different identifiers with different resolved dependencies materialize separate shared buckets`(version: GradleVersion) {
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
                    assertEquals(
                        2,
                        sharedDumpFingerprintDirs(fuzzProjectName, buzzProjectName).size,
                        "Different fingerprints should produce separate shared dump directories"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `kmp maps consumers with different identifiers run local dump tasks but share one xcodebuild bucket`(version: GradleVersion) {
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
                    val sharedDumpDir = sharedDumpFingerprintDirs(leftProjectName, rightProjectName).single()

                    assertEquals(2, dumpTasks.size, "Both consumers should keep their own local dump task")
                    assertTasksExecuted(dumpTasks)
                    assertLocalDumpDirsCopiedFromSharedBucket(
                        sharedDumpDir,
                        localIphoneosDumpDir(leftProjectName),
                        localIphoneosDumpDir(rightProjectName),
                    )
                    assertTrue(
                        sharedDumpDir.resolve("clang_args_dump").walkTopDown().any { it.isFile },
                        "The single shared bucket should contain the actual xcodebuild clang dump before local tasks copy it"
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
                    assertEquals(
                        2,
                        sharedDumpFingerprintDirs(fuzzProjectName, buzzProjectName).size,
                        "Different fingerprints should materialize two shared dump directories"
                    )
                }

                build(
                    ":$fuzzProjectName:convertSyntheticImportProjectIntoDefFileIphoneos", "-P${useSameFingerprintVariable}=true",
                    ":$buzzProjectName:convertSyntheticImportProjectIntoDefFileIphoneos", "-P${useSameFingerprintVariable}=true",
                ) {
                    val dumpTasks = findTasksByPattern(Regex(":(${fuzzProjectName}|${buzzProjectName}):dumpXcodebuildArgsIphoneos"))

                    assertEquals(2, dumpTasks.size, "Using the same fingerprint still keeps one local dump task per project")
                    assertTasksExecuted(dumpTasks)
                    assertEquals(
                        3,
                        sharedDumpFingerprintDirs(fuzzProjectName, buzzProjectName).size,
                        "Changing to one shared fingerprint should add exactly one new shared dump directory"
                    )
                }
            }
        }
    }
}

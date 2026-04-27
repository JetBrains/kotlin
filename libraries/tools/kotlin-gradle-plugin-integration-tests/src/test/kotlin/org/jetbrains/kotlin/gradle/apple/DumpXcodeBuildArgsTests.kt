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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.SyntheticProductType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PackageResolvedSynchronization
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.TransitiveSwiftPMDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.assertDirectoryExists
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.assertTasksAreNotInTaskGraph
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
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.test.assertEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class DumpXcodeBuildArgsTests : KGPBaseTest() {

    @GradleTest
    fun `smoke test - xcodebuild args are dumped into task output directory`(version: GradleVersion) {
        project("empty", version) {
            val stubTrackedFiles = projectPath.resolve("trackedFilesStub").also { it.createFile() }.toFile()
            val packageOne = projectPath.resolve("packageOne").also { it.createDirectories() }.toFile()
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
                    filesToTrackFromLocalPackages.set(stubTrackedFiles)
                    syntheticImportProjectRoot.set(packageGeneration.map { it.syntheticImportProjectRoot.get() })
                    swiftPMDependenciesCheckout.set(project.layout.buildDirectory.dir("checkout"))
                    dumpedXcodeBuildArgsDir.set(
                        project.layout.buildDirectory.dir("kotlin/customSwiftImportDump/iphonesimulator")
                    )
                }
            }

            build("packageDumpArgs")

            val dumpDir = projectPath.resolve("build/kotlin/customSwiftImportDump/iphonesimulator")
            assertDirectoryExists(dumpDir)
            assertFileExists(dumpDir.resolve("clangDump.sh"))
            assertFileExists(dumpDir.resolve("ldDump.sh"))
            assertDirectoryExists(dumpDir.resolve("clang_args_dump"))
            assertDirectoryExists(dumpDir.resolve("ld_args_dump"))
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `same fingerprint across projects reuses one dump task`(version: GradleVersion) {
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val repoName = "SharedPackage"

        project("empty", version) {
            withLockFileFixture{
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

                    // Matching SwiftPM closures should collapse to one owning dump task even though both projects still convert locally.
                    assertEquals(1, dumpTasks.size)

                    assertTasksExecuted(dumpTasks)

                    assertTasksAreNotInTaskGraph(
                        *(setOf(
                            ":$fuzzProjectName:dumpXcodebuildArgsIphoneos",
                            ":$buzzProjectName:dumpXcodebuildArgsIphoneos",
                        ) - dumpTasks).toTypedArray()
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `different fingerprints across projects keep separate dump tasks`(version: GradleVersion) {
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val repoName = "SharedPackage"

        project("empty", version) {
            withLockFileFixture{
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

                    assertTasksExecuted(
                        dumpTasks
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

        val useSameFingerprintVariable = "useSameFingerprint"

        project("empty", version) {
            withLockFileFixture{
                val sharedRepo = repoRef(repoName).also { createRepo(it.name, listOf("1.0.0", "1.0.1", "1.0.2")) }

                initSwiftPmProject(cacheDirFile) {}

                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        if (project.providers.gradleProperty("useSameFingerprint").isPresent) {
                            swiftPMDependencies {
                                packageResolvedSynchronization = PackageResolvedSynchronization.Identifier("fuzzLock")
                                swiftPackage(
                                    url = url(sharedRepo.url),
                                    version = exact("1.0.0"),
                                    products = listOf(product(sharedRepo.name)),
                                )
                            }
                        }else{
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
                                packageResolvedSynchronization = PackageResolvedSynchronization.Identifier("buzzLock")
                                swiftPackage(
                                    url = url(sharedRepo.url),
                                    version = exact("1.0.0"),
                                    products = listOf(product(sharedRepo.name)),
                                )
                            }
                        }else{
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

                    assertTasksExecuted(
                        dumpTasks
                    )
                }

                build(
                    ":$fuzzProjectName:convertSyntheticImportProjectIntoDefFileIphoneos", "-P${useSameFingerprintVariable}=true",
                    ":$buzzProjectName:convertSyntheticImportProjectIntoDefFileIphoneos", "-P${useSameFingerprintVariable}=true",
                ) {
                    val dumpTasks = findTasksByPattern(Regex(":(${fuzzProjectName}|${buzzProjectName}):dumpXcodebuildArgsIphoneos"))

                    assertEquals(1, dumpTasks.size, "Using the same fingerprint task should produce one dump task")

                    assertTasksExecuted(
                        dumpTasks
                    )
                }
            }
        }
    }
}

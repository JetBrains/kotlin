/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.api.tasks.StopExecutionException
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.register
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.createKotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.Companion.SyntheticProductType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SHARED_CHECKOUT_DIR
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SHARED_SYNTHETIC_PACKAGE_DIR
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependencyIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.TransitiveSwiftPMMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.assertFileNotExists
import org.jetbrains.kotlin.gradle.testbase.assertFilesContentEquals
import org.jetbrains.kotlin.gradle.testbase.assertFilesExist
import org.jetbrains.kotlin.gradle.testbase.assertOutputContains
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.assertTasksUpToDate
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectories
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.deleteRecursively
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class FetchSyntheticImportProjectPackagesTests : KGPBaseTest() {

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `fetch task - invalidates - on subpackage manifest changes`(version: GradleVersion) {
        project("empty", version) {
            plugins {
                kotlin("multiplatform").apply(false)
            }

            buildScriptInjection {
                project.createKotlinExtension(KotlinMultiplatformExtension::class)
                val extension = project.locateOrRegisterSwiftPMDependenciesExtension()
                val generation = project.tasks.register<GenerateSyntheticLinkageImportProject>("packageGeneration") {
                    configureWithExtension(extension)
                    konanTargets.set(setOf(KonanTarget.IOS_ARM64))
                    transitiveSwiftPMMetadata.set(
                        TransitiveSwiftPMMetadata(
                            mapOf(
                                SwiftPMDependencyIdentifier("dep", true) to SwiftPMImportMetadata(
                                    konanTargets = setOf("ios_arm64"),
                                    iosDeploymentVersion = "123.0",
                                    macosDeploymentVersion = "234.0",
                                    watchosDeploymentVersion = null,
                                    tvosDeploymentVersion = null,
                                    isModulesDiscoveryEnabled = true,
                                    dependencies = setOf(
                                        SwiftPMDependency.Remote(
                                            repository = SwiftPMDependency.Remote.Repository.Url("https://foo.bar/baz"),
                                            version = SwiftPMDependency.Remote.Version.Exact(project.property("transitiveDepVersion") as String),
                                            products = listOf(
                                                SwiftPMDependency.Product("dep"),
                                            ),
                                            cinteropClangModules = emptyList(),
                                            packageName = "baz",
                                            traits = setOf()
                                        )
                                    ),
                                ),
                            )
                        )
                    )
                    syntheticProductType.set(SyntheticProductType.INFERRED)
                }

                project.tasks.register<FetchSyntheticImportProjectPackages>("packageFetch") {
                    syntheticImportProjectRoot.set(generation.map { it.syntheticImportProjectRoot.get() })
                    doFirst { throw StopExecutionException() }
                }
            }

            build("packageFetch", "-PtransitiveDepVersion=1.0.0") {
                // For some reason "doFirst { throw StopExecutionException() }" prevents regular task(path) from returning the task path
                assert(tasks.single { it.path == ":packageFetch" }.outcome == TaskOutcome.SUCCESS)
            }
            build("packageFetch", "-PtransitiveDepVersion=1.0.0") {
                assert(tasks.single { it.path == ":packageFetch" }.outcome == TaskOutcome.UP_TO_DATE)
            }
            build("packageFetch", "-PtransitiveDepVersion=1.0.1") {
                assert(tasks.single { it.path == ":packageFetch" }.outcome == TaskOutcome.SUCCESS)
                assertOutputContains("subpackages/dep/Package.swift has changed")
            }
        }
    }

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `KT-85807 - fetch task - re-executes when checkout is removed`(version: GradleVersion) {
        project("empty", version) {
            withLockFileFixture {
                val repoAName = "TestPackageA"

                createRepo(repoAName, listOf("1.0.0"))

                val repoA = repoRef(repoAName)
                val fetchTask = ":${FetchSyntheticImportProjectPackages.TASK_NAME}"
                val checkoutDir = projectPath.resolve("build/kotlin/swiftPMCheckout")
                val workspaceStateJson = checkoutDir.resolve("workspace-state.json")

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repoA.url),
                            version = from("1.0.0"),
                            products = listOf(product(repoA.name)),
                        )
                    }
                }

                build(FetchSyntheticImportProjectPackages.TASK_NAME) {
                    assertTasksExecuted(fetchTask)
                    assertFileExists(
                        workspaceStateJson,
                        "Fetch task should create workspace-state.json in checkout directory"
                    )
                }

                build(FetchSyntheticImportProjectPackages.TASK_NAME) {
                    assertTasksUpToDate(fetchTask)
                }

                checkoutDir.deleteRecursively()
                assertFileNotExists(workspaceStateJson)

                build(FetchSyntheticImportProjectPackages.TASK_NAME) {
                    assertTasksExecuted(fetchTask)
                    assertFileExists(workspaceStateJson)
                }
            }
        }
    }

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `fetch task uses same fingerprint generated by packageFingerprint task`(version: GradleVersion) {
        project("empty", version) {
            withLockFileFixture {
                val mapsRepo = repoRef("Maps").also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(mapsRepo.url), version = exact("1.0.0"), products = listOf(product(mapsRepo.name))
                        )
                    }

                }

                build(
                    ":${FetchSyntheticImportProjectPackages.TASK_NAME}"
                ) {
                    val rootSyntheticPackageHash = projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH).readText().trim().split("\n")[1]

                    val syntheticPackage = projectPath.resolve(SHARED_SYNTHETIC_PACKAGE_DIR).resolve(rootSyntheticPackageHash)
                        .resolve("Package.resolved")
                    val sharedCheckoutDir = projectPath.resolve(SHARED_CHECKOUT_DIR).resolve(rootSyntheticPackageHash)
                        .resolve("workspace-state.json")

                    assertFilesExist(
                        syntheticPackage, sharedCheckoutDir
                    )
                }
            }
        }
    }

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `fetch tasks with various fingerprints uses correct checkout directories`(version: GradleVersion) {
        val useMapsRepo = "useMaps"

        project("empty", version) {
            withLockFileFixture {
                val mapsRepo = repoRef("Maps").also { createRepo(it.name, listOf("1.0.0", "1.0.1")) }
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

                val kmpMapsConsumer = project("empty", gradleVersion) {
                    initSwiftPmProject(cacheDirFile) {
                        sourceSets.getByName("iosArm64Main").dependencies {
                            implementation(project(":kmpMapsProject"))
                        }
                    }
                }
                include(kmpMapsConsumer, "kmpMapsConsumer")

                /**
                 * The root project uses crypto
                 * The producer maps 1.0.0
                 * The consumer depends on producer so maps 1.0.0
                 */

                val rootSyntheticPackageFingerprintFile = projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)
                val consumerSyntheticPackageFingerprintFile =
                    kmpMapsConsumer.projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)
                build(
                    ":${FetchSyntheticImportProjectPackages.TASK_NAME}",
                    ":kmpMapsConsumer:${FetchSyntheticImportProjectPackages.TASK_NAME}"
                ) {

                    val rootSyntheticPackageHash = rootSyntheticPackageFingerprintFile.readText().trim().split("\n")[1]
                    val consumerSyntheticPackageHash =
                        consumerSyntheticPackageFingerprintFile.readText().trim().split("\n")[1]

                    assertNotEquals(
                        rootSyntheticPackageHash, consumerSyntheticPackageHash, "Different dependency graph should produce different hash"
                    )

                    val rootSyntheticPackageResolved =
                        projectPath.resolve(SHARED_SYNTHETIC_PACKAGE_DIR).resolve(rootSyntheticPackageHash)
                            .resolve("Package.resolved")

                    val rootLocalPackageResolved = projectPath.resolve("build/kotlin/swiftImport/Package.resolved")

                    val rootCheckoutDirWorkspaceState =
                        projectPath.resolve(SHARED_CHECKOUT_DIR).resolve(rootSyntheticPackageHash)
                            .resolve("workspace-state.json")

                    val rootLocalWorkspaceState = projectPath.resolve("build/kotlin/swiftPMCheckout/workspace-state.json")

                    val consumerSyntheticPackageResolved =
                        projectPath.resolve(SHARED_SYNTHETIC_PACKAGE_DIR).resolve(consumerSyntheticPackageHash)
                            .resolve("Package.resolved")

                    val consumerLocalPackageResolved = kmpMapsConsumer.projectPath.resolve("build/kotlin/swiftImport/Package.resolved")

                    val consumerCheckoutDirWorkspaceState =
                        projectPath.resolve(SHARED_CHECKOUT_DIR).resolve(consumerSyntheticPackageHash)
                            .resolve("workspace-state.json")

                    val consumerLocalWorkspaceState =
                        kmpMapsConsumer.projectPath.resolve("build/kotlin/swiftPMCheckout/workspace-state.json")


                    assertFilesContentEquals(
                        rootLocalPackageResolved, rootSyntheticPackageResolved
                    )

                    assertFilesContentEquals(
                        rootLocalWorkspaceState, rootCheckoutDirWorkspaceState
                    )

                    assertFilesContentEquals(
                        consumerLocalPackageResolved, consumerSyntheticPackageResolved
                    )

                    assertFilesContentEquals(
                        consumerLocalWorkspaceState, consumerCheckoutDirWorkspaceState
                    )
                }

                build(
                    ":${FetchSyntheticImportProjectPackages.TASK_NAME}",
                    "-P${useMapsRepo}=true",
                    ":kmpMapsConsumer:${FetchSyntheticImportProjectPackages.TASK_NAME}"
                ) {
                    val rootSyntheticPackageHash = rootSyntheticPackageFingerprintFile.readText().trim().split("\n")[1]
                    val consumerSyntheticPackageHash =
                        consumerSyntheticPackageFingerprintFile.readText().trim().split("\n")[1]

                    assertEquals(
                        rootSyntheticPackageHash, consumerSyntheticPackageHash, "Same dependency graph should produce same hash"
                    )

                    val rootSyntheticPackageResolved =
                        projectPath.resolve(SHARED_SYNTHETIC_PACKAGE_DIR).resolve(rootSyntheticPackageHash)
                            .resolve("Package.resolved")

                    val rootLocalPackageResolved = projectPath.resolve("build/kotlin/swiftImport/Package.resolved")

                    val rootCheckoutDirWorkspaceState =
                        projectPath.resolve(SHARED_CHECKOUT_DIR).resolve(rootSyntheticPackageHash)
                            .resolve("workspace-state.json")

                    val rootLocalWorkspaceState = projectPath.resolve("build/kotlin/swiftPMCheckout/workspace-state.json")

                    val consumerSyntheticPackageResolved =
                        projectPath.resolve(SHARED_SYNTHETIC_PACKAGE_DIR).resolve(rootSyntheticPackageHash)
                            .resolve("Package.resolved")

                    val consumerLocalPackageResolved = kmpMapsConsumer.projectPath.resolve("build/kotlin/swiftImport/Package.resolved")

                    val consumerCheckoutDirWorkspaceState =
                        projectPath.resolve(SHARED_CHECKOUT_DIR).resolve(rootSyntheticPackageHash)
                            .resolve("workspace-state.json")

                    val consumerLocalWorkspaceState =
                        kmpMapsConsumer.projectPath.resolve("build/kotlin/swiftPMCheckout/workspace-state.json")


                    assertEqualDirectories(
                        rootSyntheticPackageResolved.parent.toFile(), consumerSyntheticPackageResolved.parent.toFile(),
                        //They are expected to be same so should be completely same
                        forgiveExtraFiles = false
                    )

                    assertEqualDirectories(
                        rootCheckoutDirWorkspaceState.parent.toFile(), consumerCheckoutDirWorkspaceState.parent.toFile(),
                        //They are expected to be same so should be completely same
                        forgiveExtraFiles = false
                    )

                    assertFilesContentEquals(
                        rootLocalPackageResolved, rootSyntheticPackageResolved
                    )

                    assertFilesContentEquals(
                        rootLocalWorkspaceState, rootCheckoutDirWorkspaceState
                    )

                    assertFilesContentEquals(
                        consumerLocalPackageResolved, consumerSyntheticPackageResolved
                    )

                    assertFilesContentEquals(
                        consumerLocalWorkspaceState, consumerCheckoutDirWorkspaceState
                    )

                    assertFilesContentEquals(
                        rootLocalPackageResolved, consumerLocalPackageResolved
                    )

                    assertFilesContentEquals(
                        rootLocalWorkspaceState, consumerLocalWorkspaceState
                    )
                }


                build(
                    ":${FetchSyntheticImportProjectPackages.TASK_NAME}",
                    ":kmpMapsConsumer:${FetchSyntheticImportProjectPackages.TASK_NAME}"
                ) {

                    val rootSyntheticPackageHash = rootSyntheticPackageFingerprintFile.readText().trim().split("\n")[1]
                    val consumerSyntheticPackageHash = consumerSyntheticPackageFingerprintFile.readText().trim().split("\n")[1]

                    assertNotEquals(
                        rootSyntheticPackageHash, consumerSyntheticPackageHash, "Different dependency graph should produce different hash"
                    )

                    val rootSyntheticPackageResolved =
                        projectPath.resolve(SHARED_SYNTHETIC_PACKAGE_DIR).resolve(rootSyntheticPackageHash)
                            .resolve("Package.resolved")

                    val rootLocalPackageResolved = projectPath.resolve("build/kotlin/swiftImport/Package.resolved")

                    val rootCheckoutDirWorkspaceState =
                        projectPath.resolve(SHARED_CHECKOUT_DIR).resolve(rootSyntheticPackageHash)
                            .resolve("workspace-state.json")

                    val rootLocalWorkspaceState = projectPath.resolve("build/kotlin/swiftPMCheckout/workspace-state.json")


                    assertFilesContentEquals(
                        rootLocalPackageResolved, rootSyntheticPackageResolved
                    )

                    assertFilesContentEquals(
                        rootLocalWorkspaceState, rootCheckoutDirWorkspaceState
                    )
                }
            }
        }
    }
}


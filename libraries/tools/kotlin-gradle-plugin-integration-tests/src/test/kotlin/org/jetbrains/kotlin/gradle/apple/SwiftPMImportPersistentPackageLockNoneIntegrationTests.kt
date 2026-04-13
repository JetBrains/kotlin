/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PackageResolvedSynchronization
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SyncPackageResolvedTask
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.include
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.*


@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)

@DisplayName("SwiftPM import Persistent Package.resolved integration tests")
@SwiftPMImportGradlePluginTests
@GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
class SwiftPMImportPersistentPackageLockNoneIntegrationTests : KGPBaseTest() {

    /**
     * Verifies that resolving remote SwiftPM git packages writes a Package.resolved file at the project project directory.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `fetchSyntheticImportProjectPackages writes Package_resolved at project directory when remote git packages are resolved`(version: GradleVersion) {
        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.None
            ) {
                val repoAName = "TestPackageA"
                val repoBName = "TestPackageB"

                createRepo(repoAName, listOf("1.0.0"))
                createRepo(repoBName, listOf("1.0.0"))

                val repoA = repoRef(repoAName)
                val repoB = repoRef(repoBName)

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repoA.url), version = from("1.0.0"), products = listOf(product(repoA.name))
                        )
                        swiftPackage(
                            url = url(repoB.url), version = from("1.0.0"), products = listOf(product(repoB.name))
                        )

                        packageResolvedSynchronization = noSynchronization()

                    }

                }

                assertFileNotExists(persistedPackageResolvedSyncPath)

                build("fetchSyntheticImportProjectPackages") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath, projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"), listOf(
                            repoA to "1.0.0",
                            repoB to "1.0.0",
                        )
                    )

                    //because we now also delete the synthetic package resolved file, if there is not persisted Package.resolved
                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_PERSISTED_PACKAGE_RESOLVED_TO_SYNTHETIC_TASK_NAME}")

                    // after creation
                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_SYNTHETIC_PACKAGE_RESOLVED_TO_PERSISTED_TASK_NAME}")
                }
            }
        }
    }

    /**
     * Ensures that when using the `from` version key, the latest compatible version is written to the project directory Package_resolved.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `fetchSyntheticImportProjectPackages writes latest version to Package_resolved at project directory when version key is from`(
        version: GradleVersion,
    ) {
        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.None
            ) {
                val repoName = "TestPackage"

                createRepo(repoName, listOf("1.0.0", "1.0.1"))

                val repo = repoRef(repoName)

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repo.url), version = from("1.0.0"), products = listOf(product(repo.name))
                        )

                        packageResolvedSynchronization = noSynchronization()
                    }
                }

                assertFileNotExists(persistedPackageResolvedSyncPath)

                build("fetchSyntheticImportProjectPackages") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath,
                        projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"),
                        listOf(
                            repo to "1.0.1",
                        )
                    )

                    //because we now also delete the synthetic package resolved file, if there is not persisted Package.resolved
                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_PERSISTED_PACKAGE_RESOLVED_TO_SYNTHETIC_TASK_NAME}")

                    // after creation
                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_SYNTHETIC_PACKAGE_RESOLVED_TO_PERSISTED_TASK_NAME}")
                }
            }
        }
    }

    /**
     * Ensures a newer compatible tag is not picked when a project directory Package_resolved already locks a version.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `fetchSyntheticImportProjectPackages keeps locked version in Package_resolved at project directory when new tag is released and project directory lock file exists`(
        version: GradleVersion,
    ) {

        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.None
            ) {
                val repoName = "TestPackage"

                createRepo(repoName, listOf("1.0.0"))

                val repo = repoRef(repoName)

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repo.url), version = from("1.0.0"), products = listOf(product(repo.name))
                        )

                        packageResolvedSynchronization = noSynchronization()
                    }
                }
                build("fetchSyntheticImportProjectPackages") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath, projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"), listOf(
                            repo to "1.0.0",
                        )
                    )

                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_PERSISTED_PACKAGE_RESOLVED_TO_SYNTHETIC_TASK_NAME}")

                    // after creation
                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_SYNTHETIC_PACKAGE_RESOLVED_TO_PERSISTED_TASK_NAME}")
                }

                releaseTag(repoName, "1.0.1")

                build("fetchSyntheticImportProjectPackages") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath, projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"), listOf(
                            repo to "1.0.0",
                        )
                    )

                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_PERSISTED_PACKAGE_RESOLVED_TO_SYNTHETIC_TASK_NAME}")
                    assertTasksUpToDate(":${SyncPackageResolvedTask.SYNC_SYNTHETIC_PACKAGE_RESOLVED_TO_PERSISTED_TASK_NAME}")
                }

            }

        }
    }


    /**
     * Ensures a newer compatible tag is not picked when a project directory Package_resolved already locks a version.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `fetchSyntheticImportProjectPackages project directory Package_resolve transitively includes swiftPM packages of subproject dependencies`(
        version: GradleVersion,
    ) {

        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.None
            ) {

                val subProjectPackageName = "TestSubPackage"
                createRepo(subProjectPackageName, listOf("1.0.0"))
                val repoSub = repoRef(subProjectPackageName)

                val mainProjectPackageName = "TestMainPackage"
                createRepo(mainProjectPackageName, listOf("1.0.0"))
                val repoMain = repoRef(mainProjectPackageName)

                val subProject = project("empty", version) {
                    withLockFileFixture {
                        initSwiftPmProject(cacheDirFile) {
                            swiftPMDependencies {
                                swiftPackage(
                                    url = url(repoSub.url), version = from("1.0.0"), products = listOf(product(repoSub.name))
                                )

                                packageResolvedSynchronization = noSynchronization()
                            }
                        }
                    }
                }

                initSwiftPmProject(cacheDirFile) {

                    sourceSets.appleMain.dependencies {
                        api(project(":subProject"))
                    }

                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repoMain.url), version = from("1.0.0"), products = listOf(product(repoMain.name))
                        )

                        packageResolvedSynchronization = noSynchronization()

                    }
                }


                include(subProject, "subProject")

                build("fetchSyntheticImportProjectPackages") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath, projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"), listOf(
                            repoMain to "1.0.0",
                            repoSub to "1.0.0",
                        )
                    )

                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_PERSISTED_PACKAGE_RESOLVED_TO_SYNTHETIC_TASK_NAME}")
                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_SYNTHETIC_PACKAGE_RESOLVED_TO_PERSISTED_TASK_NAME}")
                }
            }
        }
    }


    /**
     * Ensures that if the project directory Package_resolved is removed after a newer tag is released, the next run locks to the latest tag.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `fetchSyntheticImportProjectPackages writes latest tag to Package_resolved at project directory when it is removed after newer tag is released`(
        version: GradleVersion,
    ) {
        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.None
            ) {
                val repoName = "TestPackage"

                createRepo(repoName, listOf("1.0.0"))

                val repo = repoRef(repoName)

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repo.url), version = from("1.0.0"), products = listOf(product(repo.name))
                        )

                        packageResolvedSynchronization = noSynchronization()
                    }
                }

                build("fetchSyntheticImportProjectPackages") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath, projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"), listOf(
                            repo to "1.0.0",
                        )
                    )
                }

                releaseTag(repoName, "1.0.1")

                /**
                 * No pinned versions anymore, so the project directory Package_resolved should be deleted to allow picking up the newer tag on the next run.
                 */
                persistedPackageResolvedSyncPath.deleteIfExists()


                // FIXME: KT-85078 This clean step shouldn't be required
                build(
                    "clean"
                )

                assertFileNotExists(persistedPackageResolvedSyncPath, "Project directory Package.resolved should be deleted")


                build("fetchSyntheticImportProjectPackages") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath, projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"), listOf(
                            repo to "1.0.1",
                        )
                    )

                    assertTasksUpToDate(":${SyncPackageResolvedTask.SYNC_PERSISTED_PACKAGE_RESOLVED_TO_SYNTHETIC_TASK_NAME}")
                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_SYNTHETIC_PACKAGE_RESOLVED_TO_PERSISTED_TASK_NAME}")
                }

            }

        }
    }

    /**
     * Verifies that switching the version key from exact to from does not change the locked versions in Package_resolved.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `fetchSyntheticImportProjectPackages locks versions in Package_resolved when version key changes from exact to from`(version: GradleVersion) {

        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.None
            ) {

                val useFromVersionKey = "useFromVersionKey"
                val useExactVersionKey = "useExactVersionKey"

                val repoName = "TestPackage"

                createRepo(repoName, listOf("1.0.0", "1.0.1"))

                val repo = repoRef(repoName)

                initSwiftPmProject(cacheDirFile) {
                    if (project.hasProperty(useFromVersionKey)) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(repo.url), version = from("1.0.0"), products = listOf(product(repoName))
                            )

                            packageResolvedSynchronization = noSynchronization()
                        }
                    }
                    if (project.hasProperty(useExactVersionKey)) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(repo.url), version = exact("1.0.0"), products = listOf(product(repoName))
                            )

                            packageResolvedSynchronization = noSynchronization()
                        }
                    }
                }
                build("fetchSyntheticImportProjectPackages", "-P${useExactVersionKey}=true") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath, projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"), listOf(
                            repo to "1.0.0",
                        )
                    )
                }

                build("fetchSyntheticImportProjectPackages", "-P${useFromVersionKey}=true") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath, projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"), listOf(
                            repo to "1.0.0",
                        )
                    )

                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_PERSISTED_PACKAGE_RESOLVED_TO_SYNTHETIC_TASK_NAME}")
                    assertTasksUpToDate(":${SyncPackageResolvedTask.SYNC_SYNTHETIC_PACKAGE_RESOLVED_TO_PERSISTED_TASK_NAME}")
                }

            }
        }
    }

    /**
     * Verifies that switching the version key from exact to from does not change the locked versions in Package_resolved.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `fetchSyntheticImportProjectPackages updates versions in Package_resolved when version key changes from from to exact`(version: GradleVersion) {

        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.None
            ) {

                val useFromVersionKey = "useFromVersionKey"
                val useExactVersionKey = "useExactVersionKey"

                val repoName = "TestPackage"

                createRepo(repoName, listOf("1.0.0", "1.0.1"))

                val repo = repoRef(repoName)

                initSwiftPmProject(cacheDirFile) {
                    if (project.hasProperty(useFromVersionKey)) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(repo.url), version = from("1.0.0"), products = listOf(product(repoName))
                            )
                            packageResolvedSynchronization = noSynchronization()
                        }
                    }
                    if (project.hasProperty(useExactVersionKey)) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(repo.url), version = exact("1.0.0"), products = listOf(product(repoName))
                            )
                            packageResolvedSynchronization = noSynchronization()
                        }
                    }
                }
                build("fetchSyntheticImportProjectPackages", "-P${useFromVersionKey}=true") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath, projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"), listOf(
                            repo to "1.0.1",
                        )
                    )
                }

                build("fetchSyntheticImportProjectPackages", "-P${useExactVersionKey}=true") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath, projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"), listOf(
                            repo to "1.0.0",
                        )
                    )

                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_PERSISTED_PACKAGE_RESOLVED_TO_SYNTHETIC_TASK_NAME}")
                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_SYNTHETIC_PACKAGE_RESOLVED_TO_PERSISTED_TASK_NAME}")

                }
            }
        }
    }

    /**
     * Ensures cinterop task respects the existing project directory lock: cleaning build does not change locked versions when project directory Package_resolved exists.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `cinteropSwiftPMImportIosArm64 keeps project directory lock semantics when build directory is removed and Package_resolved at project directory still exists`(
        version: GradleVersion,
    ) {

        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.None
            ) {
                val repoName = "TestPackage"

                createRepo(repoName, listOf("1.0.0"))

                val repo = repoRef(repoName)

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repo.url), version = from("1.0.0"), products = listOf(product(repoName))
                        )
                        packageResolvedSynchronization = noSynchronization()
                    }
                }
                build("fetchSyntheticImportProjectPackages") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath, projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"),
                        listOf(
                            repo to "1.0.0",
                        )
                    )
                }

                releaseTag(repoName, "1.0.1")

                build(
                    "clean"
                )

                assertFileExists(persistedPackageResolvedSyncPath, "Project directory Package.resolved should still exist")

                build("cinteropSwiftPMImportIosArm64") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath,
                        projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"), listOf(
                            repo to "1.0.0",
                        )
                    )
                    assertTasksExecuted(":${SyncPackageResolvedTask.SYNC_PERSISTED_PACKAGE_RESOLVED_TO_SYNTHETIC_TASK_NAME}")
                }
            }
        }
    }
}





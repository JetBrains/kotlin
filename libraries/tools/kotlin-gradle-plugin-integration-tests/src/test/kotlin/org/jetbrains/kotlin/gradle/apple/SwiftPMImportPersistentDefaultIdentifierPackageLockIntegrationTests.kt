/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PackageResolvedSynchronization
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.include
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@DisplayName("SwiftPM import default identifier synchronization integration tests")
@SwiftPMImportGradlePluginTests
@GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
class SwiftPMImportPersistentDefaultIdentifierPackageLockIntegrationTests : KGPBaseTest() {

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `default identifier synchronization aggregates dependencies from fuzz and buzz into one root umbrella lock`(version: GradleVersion) {
        val identifier = "default"
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val fuzzRepoName = "FuzzPackage"
        val buzzRepoName = "BuzzPackage"

        project("empty", version) {
            withLockFileFixture {
                val fuzzRepo = repoRef(fuzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }
                val buzzRepo = repoRef(buzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {
                    // no direct deps in root, just initialize plugin infrastructure
                    swiftPMDependencies {
                        packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(identifier)
                    }
                }

                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(fuzzRepo.url),
                                version = from("1.0.0"),
                                products = listOf(product(fuzzRepo.name)),
                            )
                        }
                    }
                }

                val buzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(buzzRepo.url),
                                version = from("1.0.0"),
                                products = listOf(product(buzzRepo.name)),
                            )
                        }
                    }
                }

                include(fuzzProject, fuzzProjectName)
                include(buzzProject, buzzProjectName)

                build(":$fuzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {
                    // Root Package.locked contains both deps
                    assertResolvedVersions(
                        persistedPackageResolved = projectPath.resolve(".swiftpm-locks/$identifier/swiftImport/Package.resolved"),
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                            buzzRepo to "1.0.0",
                        ),
                    )

                    // fuzz only contains its own deps.
                    assertResolvedVersions(
                        persistedPackageResolved = fuzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = fuzzProject.projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                        ),
                    )
                }

                build(":$buzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {
                    // Root Package.locked contains both deps
                    assertResolvedVersions(
                        persistedPackageResolved = persistedPackageResolvedSyncPath,
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                            buzzRepo to "1.0.0",
                        ),
                    )

                    // buzz only contains its own deps.
                    assertResolvedVersions(
                        persistedPackageResolved = buzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = buzzProject.projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            buzzRepo to "1.0.0",
                        ),
                    )
                }
            }
        }
    }


    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `default identifier synchronization excludes projects without swiftpm dependencies from umbrella manifest`(
        version: GradleVersion,
    ) {
        val identifier = "default"
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val fuzzRepoName = "FuzzPackage"

        project("empty", version) {
            withLockFileFixture {
                val fuzzRepo = repoRef(fuzzRepoName).also {
                    createRepo(it.name, listOf("1.0.0"))
                }

                initSwiftPmProject(cacheDirFile) {

                    sourceSets.getByName("commonMain").dependencies {
                        implementation(project(":$fuzzProjectName"))
                    }

                    swiftPMDependencies {
                    }
                }

                val buzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                        }
                    }
                }

                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(fuzzRepo.url),
                                version = from("1.0.0"),
                                products = listOf(product(fuzzRepo.name)),
                            )
                        }
                    }
                    include(buzzProject, buzzProjectName)
                }

                include(fuzzProject, fuzzProjectName)

                val umbrellaPackageManifest =
                    projectPath.resolve(".swiftpm-locks/$identifier/swiftImport/Package.swift")

                build(":$fuzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {
                    assertFileExists(
                        umbrellaPackageManifest,
                        "Umbrella Package.swift should be generated"
                    )

                    val manifestDescription = describeSwiftPackage(umbrellaPackageManifest.parent)

                    print(
                        "The generate Package.swift is" +
                                "$manifestDescription"
                    )
                    assertEquals(
                        listOf("_fuzz", "_").sorted(),
                        manifestDescription.dependencies.map { it.identity }.sorted()
                    )


                    assertTrue(
                        manifestDescription.dependencies.none { it.identity == "_buzz" },
                        "Projects without SwiftPM dependencies must not be included in umbrella Package.swift"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `default identifier synchronization keeps overlapping pinned versions in root umbrella lock when two projects depend on the same package with different versions`(
        version: GradleVersion,
    ) {
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val commonRepoName = "FuzzPackage"

        project("empty", version) {
            withLockFileFixture {
                val commonRepo = repoRef(commonRepoName).also { createRepo(it.name, listOf("1.0.0", "1.0.1", "1.0.2")) }

                initSwiftPmProject(cacheDirFile) {
                    // no direct deps in root, just initialize plugin infrastructure
                    swiftPMDependencies {
                    }
                }


                // from 1.0.0
                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(commonRepo.url),
                                version = from("1.0.0"),
                                products = listOf(product(commonRepo.name)),
                            )
                        }
                    }
                }

                // exact 1.0.1
                val buzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(commonRepo.url),
                                version = exact("1.0.1"),
                                products = listOf(product(commonRepo.name)),
                            )
                        }
                    }
                }

                include(fuzzProject, fuzzProjectName)
                include(buzzProject, buzzProjectName)

                build(":$fuzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {

                    assertResolvedVersions(
                        persistedPackageResolved = persistedPackageResolvedSyncPath,
                        expectedPins = listOf(
                            // should include overlapping versions from both projects, since both are compatible with 1.0.1
                            commonRepo to "1.0.1",
                        ),
                    )

                    assertResolvedVersions(
                        persistedPackageResolved = fuzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = fuzzProject.projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            // should include overlapping versions from both projects, since both are compatible with 1.0.1
                            commonRepo to "1.0.1",
                        ),
                    )
                }
                build(":$buzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {

                    assertResolvedVersions(
                        persistedPackageResolved = persistedPackageResolvedSyncPath,
                        expectedPins = listOf(
                            // should include overlapping versions from both projects, since both are compatible with 1.0.1
                            commonRepo to "1.0.1",
                        ),
                    )

                    assertResolvedVersions(
                        persistedPackageResolved = buzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = buzzProject.projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            commonRepo to "1.0.1",
                        ),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `default identifier synchronization keeps locked versions in root umbrella lock when newer compatible tags are released`(version: GradleVersion) {
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val fuzzRepoName = "FuzzPackage"
        val buzzRepoName = "BuzzPackage"

        project("empty", version) {
            withLockFileFixture {
                val fuzzRepo = repoRef(fuzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }
                val buzzRepo = repoRef(buzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {
                    // no direct deps in root, just initialize plugin infrastructure
                    swiftPMDependencies {
                    }
                }

                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(fuzzRepo.url),
                                version = from("1.0.0"),
                                products = listOf(product(fuzzRepo.name)),
                            )
                        }
                    }
                }

                val buzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(buzzRepo.url),
                                version = from("1.0.0"),
                                products = listOf(product(buzzRepo.name)),
                            )
                        }
                    }
                }

                include(fuzzProject, fuzzProjectName)
                include(buzzProject, buzzProjectName)

                build(":$fuzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {

                    assertResolvedVersions(
                        persistedPackageResolved = persistedPackageResolvedSyncPath,
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                            buzzRepo to "1.0.0",
                        ),
                    )

                    assertResolvedVersions(
                        persistedPackageResolved = fuzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = fuzzProject.projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                        ),
                    )
                }

                build(":$buzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {

                    assertResolvedVersions(
                        persistedPackageResolved = persistedPackageResolvedSyncPath,
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                            buzzRepo to "1.0.0",
                        ),
                    )

                    assertResolvedVersions(
                        persistedPackageResolved = buzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = buzzProject.projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            buzzRepo to "1.0.0",
                        ),
                    )
                }

                releaseTag(fuzzRepo.name, "1.0.1")
                releaseTag(buzzRepo.name, "1.0.1")

                build(":$fuzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {

                    assertResolvedVersions(
                        persistedPackageResolved = persistedPackageResolvedSyncPath,
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                            buzzRepo to "1.0.0",
                        ),
                    )

                    assertResolvedVersions(
                        persistedPackageResolved = fuzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = fuzzProject.projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                        ),
                    )
                }

                build(":$buzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {

                    assertResolvedVersions(
                        persistedPackageResolved = persistedPackageResolvedSyncPath,
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                            buzzRepo to "1.0.0",
                        ),
                    )

                    assertResolvedVersions(
                        persistedPackageResolved = buzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = buzzProject.projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            buzzRepo to "1.0.0",
                        ),
                    )
                }

            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `default identifier synchronization executes umbrella generate and fetch exactly once when fuzz and buzz both request fetch in one build`(
        version: GradleVersion,
    ) {
        val identifier = "default"
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val fuzzRepoName = "FuzzPackage"
        val buzzRepoName = "BuzzPackage"

        project("empty", version) {
            withLockFileFixture {
                val fuzzRepo = repoRef(fuzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }
                val buzzRepo = repoRef(buzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                    }
                }


                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(fuzzRepo.url),
                                version = from("1.0.0"),
                                products = listOf(product(fuzzRepo.name)),
                            )
                        }
                    }
                }

                val buzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(buzzRepo.url),
                                version = from("1.0.0"),
                                products = listOf(product(buzzRepo.name)),
                            )
                        }
                    }
                }

                include(fuzzProject, fuzzProjectName)
                include(buzzProject, buzzProjectName)


                val umbrellaGenerateTasks = listOf(
                    ":${GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(identifier)}",
                    ":$fuzzProjectName:${GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(identifier)}",
                    ":$buzzProjectName:${GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(identifier)}",
                )
                val umbrellaFetchTasks = listOf(
                    ":${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(identifier)}",
                    ":$fuzzProjectName:${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(identifier)}",
                    ":$buzzProjectName:${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(identifier)}",
                )

                build(":$fuzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {

                    //count how many generate umbrella task were registered. Exactly one for each identifier
                    val registeredUmbrellaGenerateCount = umbrellaGenerateTasks.count { task(it) != null }
                    assertEquals(1, registeredUmbrellaGenerateCount, "Exactly one umbrella generate task must be registered per build")

                    val registeredUmbrellaFetchCount = umbrellaFetchTasks.count { task(it) != null }
                    assertEquals(1, registeredUmbrellaFetchCount, "Exactly one umbrella fetch task must be registered per build")

                    assertResolvedVersions(
                        persistedPackageResolved = persistedPackageResolvedSyncPath,
                        expectedPins = listOf(
                            // should include overlapping versions from both projects, since both are compatible with 1.0.1
                            fuzzRepo to "1.0.0",
                            buzzRepo to "1.0.0",
                        ),
                    )

                    assertResolvedVersions(
                        persistedPackageResolved = fuzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = fuzzProject.projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                        ),
                    )
                }

                build(":$buzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {

                    //count how many generate umbrella task were registered. Exactly one for each identifier
                    val registeredUmbrellaGenerateCount = umbrellaFetchTasks.count { task(it) != null }
                    assertEquals(1, registeredUmbrellaGenerateCount, "Exactly one umbrella generate task must be registered per build")

                    val registeredUmbrellaFetchCount = umbrellaFetchTasks.count { task(it) != null }
                    assertEquals(1, registeredUmbrellaFetchCount, "Exactly one umbrella fetch task must be registered per build")

                    assertResolvedVersions(
                        persistedPackageResolved = persistedPackageResolvedSyncPath,
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                            buzzRepo to "1.0.0",
                        ),
                    )

                    assertResolvedVersions(
                        persistedPackageResolved = buzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = buzzProject.projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            buzzRepo to "1.0.0",
                        ),
                    )
                }
            }
        }
    }
}

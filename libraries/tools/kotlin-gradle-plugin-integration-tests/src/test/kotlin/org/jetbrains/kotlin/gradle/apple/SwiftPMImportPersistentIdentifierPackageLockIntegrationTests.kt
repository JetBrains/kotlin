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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SerializeSwiftPMDependenciesMetadataForLockFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FingerprintSyntheticPackage
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FingerprintXcodeBuild
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.DumpXcodeBuildArgs
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@DisplayName("SwiftPM import identifier synchronization integration tests")
@SwiftPMImportGradlePluginTests
@GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
class SwiftPMImportPersistentIdentifierPackageLockIntegrationTests : KGPBaseTest() {

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `identifier synchronization aggregates dependencies from fuzz and buzz into one root umbrella lock`(version: GradleVersion) {
        val commonIdentifier = "common"
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val fuzzRepoName = "FuzzPackage"
        val buzzRepoName = "BuzzPackage"

        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
            ) {
                val fuzzRepo = repoRef(fuzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }
                val buzzRepo = repoRef(buzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {
                    // no direct deps in root, just initialize plugin infrastructure
                    swiftPMDependencies {
                        packageResolvedSynchronization = identifier(commonIdentifier)
                    }
                }


                val identifierGitIgnore = projectPath.resolve(".swiftpm-locks/$commonIdentifier/.gitignore")

                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
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
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
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
                        persistedPackageResolved = projectPath.resolve(".swiftpm-locks/$commonIdentifier/swiftImport/Package.resolved"),
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                            buzzRepo to "1.0.0",
                        ),
                    )

                    // fuzz only contains its own deps.
                    assertResolvedVersions(
                        persistedPackageResolved = fuzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$commonIdentifier/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                        ),
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnore,
                        "swiftPMCheckout/",
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$commonIdentifier/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            buzzRepo to "1.0.0",
                        ),
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnore,
                        "swiftPMCheckout/",
                    )
                }
            }
        }
    }


    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `identifier synchronization excludes projects without swiftpm dependencies from umbrella manifest`(
        version: GradleVersion,
    ) {
        val commonIdentifier = "common"
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val fuzzRepoName = "FuzzPackage"

        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
            ) {
                val fuzzRepo = repoRef(fuzzRepoName).also {
                    createRepo(it.name, listOf("1.0.0"))
                }

                initSwiftPmProject(cacheDirFile) {

                    sourceSets.getByName("commonMain").dependencies {
                        implementation(project(":$fuzzProjectName"))
                    }

                    swiftPMDependencies {
                        packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
                    }
                }


                val identifierGitIgnore = projectPath.resolve(".swiftpm-locks/$commonIdentifier/.gitignore")

                val buzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
                        }
                    }
                }

                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
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
                    projectPath.resolve(".swiftpm-locks/$commonIdentifier/swiftImport/Package.swift")

                build(":$fuzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {
                    assertFileExists(
                        umbrellaPackageManifest,
                        "Umbrella Package.swift should be generated"
                    )

                    val manifestDescription = describeSwiftPackage(umbrellaPackageManifest.parent)

                    assertEquals(
                        listOf("_fuzz", "_").sorted(),
                        manifestDescription.dependencies.map { it.identity }.sorted()
                    )


                    assertTrue(
                        manifestDescription.dependencies.none { it.identity == "_buzz" },
                        "Projects without SwiftPM dependencies must not be included in umbrella Package.swift"
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnore,
                        "swiftPMCheckout/",
                    )
                }

            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun   `identifier synchronization keeps overlapping pinned versions in root umbrella lock when two projects depend on the same package with different versions`(
        version: GradleVersion,
    ) {
        val commonIdentifier = "common"
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val commonRepoName = "FuzzPackage"

        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
            ) {
                val commonRepo = repoRef(commonRepoName).also { createRepo(it.name, listOf("1.0.0", "1.0.1", "1.0.2")) }

                initSwiftPmProject(cacheDirFile) {
                    // no direct deps in root, just initialize plugin infrastructure
                    swiftPMDependencies {
                        packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
                    }
                }


                val identifierGitIgnore = projectPath.resolve(".swiftpm-locks/$commonIdentifier/.gitignore")

                // from 1.0.0
                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
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
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$commonIdentifier/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            // should include overlapping versions from both projects, since both are compatible with 1.0.1
                            commonRepo to "1.0.1",
                        ),
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnore,
                        "swiftPMCheckout/",
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$commonIdentifier/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            commonRepo to "1.0.1",
                        ),
                    )
                    assertGitIgnoreEquals(
                        identifierGitIgnore,
                        "swiftPMCheckout/",
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `identifier synchronization keeps locked versions in root umbrella lock when newer compatible tags are released`(version: GradleVersion) {
        val commonIdentifier = "common"
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val fuzzRepoName = "FuzzPackage"
        val buzzRepoName = "BuzzPackage"

        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
            ) {
                val fuzzRepo = repoRef(fuzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }
                val buzzRepo = repoRef(buzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {
                    // no direct deps in root, just initialize plugin infrastructure
                    swiftPMDependencies {
                        packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
                    }
                }


                val identifierGitIgnore = projectPath.resolve(".swiftpm-locks/$commonIdentifier/.gitignore")

                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
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
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$commonIdentifier/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                        ),
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnore,
                        "swiftPMCheckout/",
                    )
                }

                build(":$buzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {

                    assertResolvedVersions(
                        persistedPackageResolved = persistedPackageResolvedSyncPath,
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                            buzzRepo to "1.0.0",
                        )
                    )

                    assertResolvedVersions(
                        persistedPackageResolved = buzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$commonIdentifier/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            buzzRepo to "1.0.0",
                        ),
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnore,
                        "swiftPMCheckout/",
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$commonIdentifier/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                        ),
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnore,
                        "swiftPMCheckout/",
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$commonIdentifier/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            buzzRepo to "1.0.0",
                        ),
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnore,
                        "swiftPMCheckout/",
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `identifier synchronization executes umbrella generate and fetch exactly once when fuzz and buzz both request fetch in one build`(
        version: GradleVersion,
    ) {
        val commonIdentifier = "common"
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val fuzzRepoName = "FuzzPackage"
        val buzzRepoName = "BuzzPackage"

        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
            ) {
                val fuzzRepo = repoRef(fuzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }
                val buzzRepo = repoRef(buzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
                    }
                }

                val identifierGitIgnore = projectPath.resolve(".swiftpm-locks/$commonIdentifier/.gitignore")

                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
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
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(commonIdentifier)
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
                    ":${GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(commonIdentifier)}",
                    ":$fuzzProjectName:${GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(commonIdentifier)}",
                    ":$buzzProjectName:${GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(commonIdentifier)}",
                )
                val umbrellaFetchTasks = listOf(
                    ":${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(commonIdentifier)}",
                    ":$fuzzProjectName:${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(commonIdentifier)}",
                    ":$buzzProjectName:${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(commonIdentifier)}",
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$commonIdentifier/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                        ),
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnore,
                        "swiftPMCheckout/",
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$commonIdentifier/swiftPMCheckout/checkouts"),
                        expectedPins = listOf(
                            buzzRepo to "1.0.0",
                        ),
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnore,
                        "swiftPMCheckout/",
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `identifier synchronization keeps fuzz and buzz isolated when they use different identifiers`(version: GradleVersion) {
        val fuzzIdentifier = "fuzzLock"
        val buzzIdentifier = "buzzLock"
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val fuzzRepoName = "FuzzPackage"
        val buzzRepoName = "BuzzPackage"

        project("empty", version) {
            withLockFileFixture {
                val fuzzRepo = repoRef(fuzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }
                val buzzRepo = repoRef(buzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {}


                val identifierGitIgnoreFuzz = projectPath.resolve(".swiftpm-locks/$fuzzIdentifier/.gitignore")
                val identifierGitIgnoreBuzz = projectPath.resolve(".swiftpm-locks/$buzzIdentifier/.gitignore")


                val fuzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(fuzzIdentifier)
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
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(buzzIdentifier)
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


                val umbrellaPackageResolvedFuzz = project.projectPath
                    .resolve(".swiftpm-locks/$fuzzIdentifier/swiftImport/Package.resolved")

                val syntheticFuzzCheckoutDir = projectPath
                    .resolve(".swiftpm-locks/$fuzzIdentifier/swiftPMCheckout/checkouts")

                val umbrellaPackageResolvedBuzz = project.projectPath
                    .resolve(".swiftpm-locks/$buzzIdentifier/swiftImport/Package.resolved")

                val syntheticBuzzCheckoutDir = projectPath
                    .resolve(".swiftpm-locks/$buzzIdentifier/swiftPMCheckout/checkouts")


                val umbrellaGenerateTaskFuzzLock =
                    ":$fuzzProjectName:${GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(fuzzIdentifier)}"


                val umbrellaGenerateTaskBuzzLock =
                    ":$buzzProjectName:${GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(buzzIdentifier)}"

                val umbrellaFetchTaskFuzzLock =
                    ":$fuzzProjectName:${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(fuzzIdentifier)}"


                val umbrellaFetchTaskBuzzLock =
                    ":$buzzProjectName:${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(buzzIdentifier)}"

                build(
                    ":$fuzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}"
                ) {

                    assertTasksExecuted(umbrellaGenerateTaskFuzzLock, umbrellaFetchTaskFuzzLock)

                    assertResolvedVersions(
                        persistedPackageResolved = umbrellaPackageResolvedFuzz,
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                        ),
                    )

                    assertResolvedVersions(
                        persistedPackageResolved = fuzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = syntheticFuzzCheckoutDir,
                        expectedPins = listOf(
                            fuzzRepo to "1.0.0",
                        ),
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnoreFuzz,
                        "swiftPMCheckout/",
                    )
                }

                build(
                    ":$buzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}",
                ) {

                    assertTasksExecuted(umbrellaGenerateTaskBuzzLock, umbrellaFetchTaskBuzzLock)

                    assertResolvedVersions(
                        persistedPackageResolved = umbrellaPackageResolvedBuzz,
                        expectedPins = listOf(
                            buzzRepo to "1.0.0",
                        ),
                    )

                    assertResolvedVersions(
                        persistedPackageResolved = buzzProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = syntheticBuzzCheckoutDir,
                        expectedPins = listOf(
                            buzzRepo to "1.0.0",
                        ),
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnoreBuzz,
                        "swiftPMCheckout/",
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `identifier synchronization ignores non-Apple consumer projects when generating umbrella lock`(
        version: GradleVersion,
    ) {
        val defaultIdentifier = "default"
        val sharedIdentifier = "shared"
        val sharedProjectName = "shared"
        val sharedRepoName = "SharedPackage"

        project("empty", version) {
            withLockFileFixture {
                val sharedRepo = repoRef(sharedRepoName).also {
                    createRepo(it.name, listOf("1.0.0"))
                }

                initJvmKmp{
                    sourceSets.getByName("commonMain").dependencies {
                        implementation(project(":$sharedProjectName"))
                    }
                }

                val sharedProject = project("empty", version) {
                    withLockFileFixture(
                        packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(sharedProjectName),
                    ) {
                        initSwiftPmProject(cacheDirFile) {
                            jvm()
                            swiftPMDependencies {
                                packageResolvedSynchronization = identifier(sharedIdentifier)

                                swiftPackage(
                                    url = url(sharedRepo.url),
                                    version = from("1.0.0"),
                                    products = listOf(product(sharedRepo.name)),
                                )
                            }
                        }
                    }
                }

                include(sharedProject, sharedProjectName)


                val umbrellaRootPackageManifest =
                    projectPath.resolve(".swiftpm-locks/$defaultIdentifier/swiftImport/Package.swift")

                val umbrellaSharedPackageManifest =
                    projectPath.resolve(".swiftpm-locks/$sharedIdentifier/swiftImport/Package.swift")

                val sharedPersistedPackageResolved = projectPath.resolve(".swiftpm-locks/$sharedIdentifier/swiftImport/Package.resolved")
                val rootPersistedPackageResolved = persistedPackageResolvedSyncPath

                build(":$sharedProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {

                    assertExactTasksInGraph(
                        ":$sharedProjectName:${SerializeSwiftPMDependenciesMetadataForLockFiles.TASK_NAME}",
                        ":$sharedProjectName:${FingerprintSyntheticPackage.TASK_NAME}",
                        ":$sharedProjectName:${GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName}",
                        ":$sharedProjectName:${
                            GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(
                                sharedIdentifier
                            )
                        }",
                        ":$sharedProjectName:${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(sharedIdentifier)}",
                        ":$sharedProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}",
                    )

                    assertFileExists(
                        umbrellaSharedPackageManifest,
                        "Umbrella Package.swift should be generated"
                    )

                    val manifestDescription = describeSwiftPackage(umbrellaSharedPackageManifest.parent)

                    assertEquals(
                        manifestDescription.dependencies.map { it.identity },
                        listOf("_$sharedProjectName").sorted(),
                        "Only Apple SwiftPM project must be included in umbrella Package.swift"
                    )

                    assertResolvedVersions(
                        persistedPackageResolved = sharedPersistedPackageResolved,
                        expectedPins = listOf(
                            sharedRepo to "1.0.0",
                        ),
                    )

                    assertResolvedVersions(
                        persistedPackageResolved = sharedProject.projectPath.resolve("build/kotlin/swiftImport/Package.resolved"),
                        checkoutRepoDir = swiftPMFingerprintCheckoutDir(sharedProject.projectPath, projectPath).resolve("checkouts"),
                        expectedPins = listOf(
                            sharedRepo to "1.0.0",
                        ),
                    )
                }


                build(":${FetchSyntheticImportProjectPackages.TASK_NAME}") {

                    assertExactTasksInGraph(
                        ":${FingerprintSyntheticPackage.TASK_NAME}",
                        ":${GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName}",
                        ":${FetchSyntheticImportProjectPackages.TASK_NAME}",
                    )


                    assertFileNotExists(
                        umbrellaRootPackageManifest,
                        "Umbrella Package.swift should not be generated"
                    )

                    assertFileNotExists(
                        rootPersistedPackageResolved,
                        "Umbrella Package.resolved should not be generated"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `link task skips all SwiftPM import tasks in unrelated project without direct or transitive SwiftPM deps when sibling uses explicit identifier alignment`(
        version: GradleVersion,
    ) {
        val alignedIdentifier = "sharedLock"
        val emptyIdentifier = "emptyLock"
        val projectWithDepsName = "projectWithDeps"
        val projectWithoutDepsName = "projectWithoutDeps"
        val repoName = "TestPackage"

        project("empty", version) {
            withLockFileFixture(
                packageResolvedSynchronization = PackageResolvedSynchronization.Identifier("rootLock")
            ) {
                val repo = repoRef(repoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        packageResolvedSynchronization = PackageResolvedSynchronization.Identifier("rootLock")
                    }
                }

                val projectWithDeps = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(alignedIdentifier)
                            swiftPackage(
                                url = url(repo.url),
                                version = from("1.0.0"),
                                products = listOf(product(repo.name)),
                            )
                        }
                    }
                }

                val projectWithoutDeps = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.Identifier(emptyIdentifier)
                        }
                    }
                }

                include(projectWithDeps, projectWithDepsName)
                include(projectWithoutDeps, projectWithoutDepsName)

                val depsGenerateUmbrellaTask =
                    ":$projectWithDepsName:${GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(alignedIdentifier)}"
                val depsFetchUmbrellaTask =
                    ":$projectWithDepsName:${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(alignedIdentifier)}"

                dumpTaskGraph(":$projectWithoutDepsName:linkDebugTestIosSimulatorArm64") {

                    assertExactSwiftImportTasksInGraph(
                        ":$projectWithoutDepsName:${SerializeSwiftPMDependenciesMetadataForLockFiles.TASK_NAME}",
                        ":$projectWithoutDepsName:${FingerprintSyntheticPackage.TASK_NAME}",
                        ":$projectWithoutDepsName:${GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName}",
                        ":$projectWithoutDepsName:${
                            GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(
                                emptyIdentifier
                            )
                        }",
                        ":$projectWithoutDepsName:${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(emptyIdentifier)}",
                        ":$projectWithoutDepsName:${FetchSyntheticImportProjectPackages.TASK_NAME}",
                        ":$projectWithoutDepsName:iosSimulatorArm64ProcessResources",
                        ":$projectWithoutDepsName:validateLocalSwiftPMDependencies",
                        ":$projectWithoutDepsName:computeLocalPackageDependencyInputFiles",
                        ":$projectWithoutDepsName:compileKotlinIosSimulatorArm64",
                        ":$projectWithoutDepsName:${lowerCamelCaseName(FingerprintXcodeBuild.TASK_NAME, "Iphonesimulator")}",
                        ":$projectWithoutDepsName:${lowerCamelCaseName(DumpXcodeBuildArgs.TASK_NAME, "Iphonesimulator")}",
                        ":$projectWithoutDepsName:convertSyntheticImportProjectIntoDefFileIphonesimulator",
                        ":$projectWithoutDepsName:iosSimulatorArm64MainKlibrary",
                        ":$projectWithoutDepsName:compileTestKotlinIosSimulatorArm64",
                        ":$projectWithoutDepsName:linkDebugTestIosSimulatorArm64",
                    )
                }

                dumpTaskGraph(":$projectWithDepsName:linkDebugTestIosSimulatorArm64") {

                    assertExactSwiftImportTasksInGraph(
                        ":$projectWithDepsName:${SerializeSwiftPMDependenciesMetadataForLockFiles.TASK_NAME}",
                        ":$projectWithDepsName:${FingerprintSyntheticPackage.TASK_NAME}",
                        ":$projectWithDepsName:${GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName}",
                        depsGenerateUmbrellaTask,
                        depsFetchUmbrellaTask,
                        ":$projectWithDepsName:${FetchSyntheticImportProjectPackages.TASK_NAME}",
                        ":$projectWithDepsName:iosSimulatorArm64ProcessResources",
                        ":$projectWithDepsName:validateLocalSwiftPMDependencies",
                        ":$projectWithDepsName:computeLocalPackageDependencyInputFiles",
                        ":$projectWithDepsName:compileKotlinIosSimulatorArm64",
                        ":$projectWithDepsName:${lowerCamelCaseName(FingerprintXcodeBuild.TASK_NAME, "Iphonesimulator")}",
                        ":$projectWithDepsName:${lowerCamelCaseName(DumpXcodeBuildArgs.TASK_NAME, "Iphonesimulator")}",
                        ":$projectWithDepsName:convertSyntheticImportProjectIntoDefFileIphonesimulator",
                        ":$projectWithDepsName:cinteropSwiftPMImportIosSimulatorArm64",
                        ":$projectWithDepsName:iosSimulatorArm64MainKlibrary",
                        ":$projectWithDepsName:compileTestKotlinIosSimulatorArm64",
                        ":$projectWithDepsName:linkDebugTestIosSimulatorArm64",
                    )
                }
            }
        }
    }
}

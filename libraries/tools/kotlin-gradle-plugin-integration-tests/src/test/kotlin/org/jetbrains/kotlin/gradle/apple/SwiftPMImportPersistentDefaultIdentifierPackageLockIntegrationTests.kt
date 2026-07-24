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
                }
                val identifierGitIgnore = projectPath.resolve(".swiftpm-locks/$identifier/.gitignore")

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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$identifier/swiftPMCheckout/checkouts"),
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$identifier/swiftPMCheckout/checkouts"),
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
                }
                val identifierGitIgnore = projectPath.resolve(".swiftpm-locks/$identifier/.gitignore")

                val buzzProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {}
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
    fun `default identifier synchronization keeps overlapping pinned versions in root umbrella lock when two projects depend on the same package with different versions`(
        version: GradleVersion,
    ) {
        val identifier = "default"
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val commonRepoName = "FuzzPackage"

        project("empty", version) {
            withLockFileFixture {
                val commonRepo = repoRef(commonRepoName).also { createRepo(it.name, listOf("1.0.0", "1.0.1", "1.0.2")) }

                initSwiftPmProject(cacheDirFile) {}

                val identifierGitIgnore = projectPath.resolve(".swiftpm-locks/$identifier/.gitignore")

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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$identifier/swiftPMCheckout/checkouts"),
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$identifier/swiftPMCheckout/checkouts"),
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
    fun `default identifier synchronization keeps locked versions in root umbrella lock when newer compatible tags are released`(version: GradleVersion) {
        val identifier = "default"
        val fuzzProjectName = "fuzz"
        val buzzProjectName = "buzz"
        val fuzzRepoName = "FuzzPackage"
        val buzzRepoName = "BuzzPackage"

        project("empty", version) {
            withLockFileFixture {
                val fuzzRepo = repoRef(fuzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }
                val buzzRepo = repoRef(buzzRepoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {}

                val identifierGitIgnore = projectPath.resolve(".swiftpm-locks/$identifier/.gitignore")

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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$identifier/swiftPMCheckout/checkouts"),
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$identifier/swiftPMCheckout/checkouts"),
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$identifier/swiftPMCheckout/checkouts"),
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$identifier/swiftPMCheckout/checkouts"),
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

                initSwiftPmProject(cacheDirFile) {}

                val identifierGitIgnore = projectPath.resolve(".swiftpm-locks/$identifier/.gitignore")

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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$identifier/swiftPMCheckout/checkouts"),
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
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$identifier/swiftPMCheckout/checkouts"),
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
    fun `jvm compilation does not materialize SwiftPM import tasks from KMP dependency with SwiftPM deps`(
        version: GradleVersion,
    ) {
        val sharedProjectName = "shared"
        val sharedRepoName = "SharedPackage"

        project("empty", version) {
            withLockFileFixture {
                val sharedRepo = repoRef(sharedRepoName).also {
                    createRepo(it.name, listOf("1.0.0"))
                }

                initSwiftPmProject(cacheDirFile) {
                    jvm()
                    sourceSets.getByName("commonMain").dependencies {
                        implementation(project(":$sharedProjectName"))
                    }
                }

                val sharedProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        jvm()
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(sharedRepo.url),
                                version = from("1.0.0"),
                                products = listOf(product(sharedRepo.name)),
                            )
                        }
                    }
                }

                include(sharedProject, sharedProjectName)

                dumpTaskGraph(":compileKotlinJvm") {

                    assertExactTaskGraph(
                        ":kmpPartiallyResolvedDependenciesChecker",
                        ":checkKotlinGradlePluginConfigurationErrors",
                        ":shared:kmpPartiallyResolvedDependenciesChecker",
                        ":shared:checkKotlinGradlePluginConfigurationErrors",
                        ":shared:compileKotlinJvm",
                        ":shared:compileJvmMainJava",
                        ":shared:jvmProcessResources",
                        ":shared:processJvmMainResources",
                        ":shared:jvmMainClasses",
                        ":shared:jvmJar",
                        ":compileKotlinJvm"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `identifier synchronization ignores non-Apple consumer projects when generating umbrella lock without swiftPM import block`(
        version: GradleVersion,
    ) {
        val defaultIdentifier = "default"
        val sharedProjectName = "shared"

        project("empty", version) {
            withLockFileFixture{

                initJvmKmp{
                    sourceSets.getByName("commonMain").dependencies {
                        implementation(project(":$sharedProjectName"))
                    }
                }

                val sharedProject = project("empty", version) {
                    withLockFileFixture{
                        initSwiftPmProject(cacheDirFile) {
                            jvm()
                        }
                    }
                }

                include(sharedProject, sharedProjectName)

                val expectedSharedGenerateUmbrellaPackageTaskName =
                    GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(defaultIdentifier)

                val expectedSharedFetchUmbrellaPackageTaskName =
                    FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(defaultIdentifier)

                build(":$sharedProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {

                    assertTasksSkipped(
                        ":$sharedProjectName:$expectedSharedGenerateUmbrellaPackageTaskName",
                        ":$sharedProjectName:$expectedSharedFetchUmbrellaPackageTaskName",
                    )

                    // umbrella generate should be picked by shared
                    assertExactTasksInGraph(
                        ":$sharedProjectName:${SerializeSwiftPMDependenciesMetadataForLockFiles.TASK_NAME}",
                        ":$sharedProjectName:${FingerprintSyntheticPackage.TASK_NAME}",
                        ":$sharedProjectName:${GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName}",
                        ":$sharedProjectName:$expectedSharedGenerateUmbrellaPackageTaskName",
                        ":$sharedProjectName:$expectedSharedFetchUmbrellaPackageTaskName",
                        ":$sharedProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}"
                    )
                }

                dumpTaskGraph(":$sharedProjectName:build")
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `default identifier synchronization overwrites identifier gitignore with checkout dir`(
        version: GradleVersion,
    ) {
        val identifier = "default"
        val fuzzProjectName = "fuzz"
        val fuzzRepoName = "FuzzPackage"

        project("empty", version) {
            withLockFileFixture {
                val fuzzRepo = repoRef(fuzzRepoName).also {
                    createRepo(it.name, listOf("1.0.0"))
                }

                initSwiftPmProject(cacheDirFile) {}
                val identifierGitIgnore = projectPath.resolve(".swiftpm-locks/$identifier/.gitignore")

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

                include(fuzzProject, fuzzProjectName)


                build(":$fuzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {
                    assertFileExists(
                        identifierGitIgnore,
                        ".gitignore should be generated at $identifierGitIgnore"
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnore,
                        "swiftPMCheckout/",
                    )
                }

                build(":$fuzzProjectName:${FetchSyntheticImportProjectPackages.TASK_NAME}") {
                    assertFileExists(
                        identifierGitIgnore,
                        "Umbrella Package.swift should be generated"
                    )

                    assertGitIgnoreEquals(
                        identifierGitIgnore,
                        "swiftPMCheckout/",
                    )
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
            withLockFileFixture {

                val identifier = "default"
                val useFromVersionKey = "useFromVersionKey"
                val useExactVersionKey = "useExactVersionKey"

                val repoName = "TestPackage"

                val packageRepo = repoRef(repoName).also {
                    createRepo(it.name, listOf("1.0.0", "1.0.1", "1.0.2"))
                }

                initSwiftPmProject(cacheDirFile) {
                    if (project.hasProperty(useFromVersionKey)) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(packageRepo.url),
                                version = from("1.0.0"),
                                products = listOf(product(repoName))
                            )
                        }
                    }
                    if (project.hasProperty(useExactVersionKey)) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(packageRepo.url), version = exact("1.0.1"), products = listOf(product(repoName))
                            )
                        }
                    }
                }
                build("fetchSyntheticImportProjectPackages", "-P${useExactVersionKey}=true") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath,
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$identifier/swiftPMCheckout/checkouts"),
                        listOf(
                            packageRepo to "1.0.1",
                        )
                    )

                    assertExactTasksInGraph(
                        ":${SerializeSwiftPMDependenciesMetadataForLockFiles.TASK_NAME}",
                        ":${FingerprintSyntheticPackage.TASK_NAME}",
                        ":${GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName}",
                        ":${GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(identifier)}",
                        ":${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(identifier)}",
                        ":${FetchSyntheticImportProjectPackages.TASK_NAME}",
                    )
                }

                build("fetchSyntheticImportProjectPackages", "-P${useFromVersionKey}=true") {
                    assertResolvedVersions(
                        persistedPackageResolvedSyncPath,
                        checkoutRepoDir = projectPath.resolve(".swiftpm-locks/$identifier/swiftPMCheckout/checkouts"),
                        listOf(
                            packageRepo to "1.0.1",
                        )
                    )

                    assertEquals(
                        listOf("2.0.0"),
                        describeSwiftPackage(projectPath.resolve(".swiftpm-locks/$identifier/swiftImport/subpackages/_"))
                            .dependencies.mapNotNull {
                                it.requirement?.range?.map { it.upperBound }
                            }.flatten(),
                        "Make sure we regenerated the package with the from constraint",
                    )

                    // Make sure we refetched
                    assertTasksExecuted(":${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(identifier)}")

                    assertExactTasksInGraph(
                        ":${SerializeSwiftPMDependenciesMetadataForLockFiles.TASK_NAME}",
                        ":${FingerprintSyntheticPackage.TASK_NAME}",
                        ":${GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName}",
                        ":${GenerateSyntheticLinkageImportProject.syntheticUmbrellaPackageGenerationTaskName(identifier)}",
                        ":${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName(identifier)}",
                        ":${FetchSyntheticImportProjectPackages.TASK_NAME}",
                    )
                }

            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `convertSyntheticImportProjectIntoDefFileIphoneos keeps swiftpm resolution isolated across subprojects` (version: GradleVersion) {

        val revenueConsumerProject = "revenueConsumer"

        project("empty", version) {
            withLockFileFixture {

                initSwiftPmProject(cacheDirFile) {
                    sourceSets.getByName("commonMain").dependencies {
                        implementation(project(":$revenueConsumerProject"))

                    }
                    swiftPMDependencies {
                        swiftPackage(
                            url = url("https://github.com/firebase/firebase-ios-sdk.git"),
                            version = from("12.5.0"),
                            products = listOf(product("FirebaseAnalytics")),
                        )
                        swiftPackage(
                            url = url("https://github.com/apple/swift-protobuf.git"),
                            version = exact("1.31.0"),
                            products = listOf(product("SwiftProtobuf")),
                        )
                    }
                }

                val revenueConsumer = project("empty", version){
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url("https://github.com/RevenueCat/purchases-ios-spm.git"),
                                version = exact("5.79.0"),
                                products = listOf(product("RevenueCat")),
                            )
                        }

                    }
                }

                include(revenueConsumer, revenueConsumerProject)

                build(":convertSyntheticImportProjectIntoDefFileIphoneos") {

                    assertTasksExecuted(
                        ":${FetchSyntheticImportProjectPackages.TASK_NAME}"
                    )

                    assertDirectoryExists(
                        projectPath.resolve("build/kotlin/swiftPMCheckout"),
                        "Root project must create its own swiftPMCheckout directory for SwiftPM dependencies"
                    )
                }

            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `compile task in unrelated project without direct or transitive SwiftPM deps does not materialize SwiftPM import tasks`(
        version: GradleVersion,
    ) {
        val rightProjectName = "right"
        val repoName = "TestPackage"

        project("empty", version) {
            withLockFileFixture {
                val repo = repoRef(repoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repo.url),
                            version = from("1.0.0"),
                            products = listOf(product(repo.name)),
                        )
                    }
                }

                val rightProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {}
                }

                include(rightProject, rightProjectName)

                dumpTaskGraph(":$rightProjectName:compileKotlinIosSimulatorArm64") {

                    assertExactSwiftImportTasksInGraph(
                        ":right:compileKotlinIosSimulatorArm64"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `link task in project without direct or transitive SwiftPM deps skips all SwiftPM import tasks when sibling has default identifier alignment enabled`(
        version: GradleVersion,
    ) {
        val leftProjectName = "left"
        val rightProjectName = "right"
        val repoName = "TestPackage"

        project("empty", version) {
            withLockFileFixture {
                val repo = repoRef(repoName).also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {}

                val leftProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(repo.url),
                                version = from("1.0.0"),
                                products = listOf(product(repo.name)),
                            )
                        }
                    }
                }

                val rightProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            packageResolvedSynchronization = PackageResolvedSynchronization.None
                        }
                    }
                }

                include(leftProject, leftProjectName)
                include(rightProject, rightProjectName)

                dumpTaskGraph(":$rightProjectName:linkDebugTestIosSimulatorArm64") {

                    assertExactSwiftImportTasksInGraph(
                        ":right:iosSimulatorArm64ProcessResources",
                        ":right:validateLocalSwiftPMDependencies",
                        ":right:computeLocalPackageDependencyInputFiles",
                        ":right:generateSyntheticLinkageSwiftPMImportProjectForCinteropsAndLdDump",
                        ":right:fetchSyntheticImportProjectPackages",
                        ":right:dumpXcodebuildArgsIphonesimulator",
                        ":right:convertSyntheticImportProjectIntoDefFileIphonesimulator",
                        ":right:compileKotlinIosSimulatorArm64",
                        ":right:iosSimulatorArm64MainKlibrary",
                        ":right:compileTestKotlinIosSimulatorArm64",
                        ":right:linkDebugTestIosSimulatorArm64",
                    )
                }

                dumpTaskGraph(":$leftProjectName:linkDebugTestIosSimulatorArm64") {

                    assertExactSwiftImportTasksInGraph(
                        ":left:validateLocalSwiftPMDependencies",
                        ":left:computeLocalPackageDependencyInputFiles",
                        ":left:fingerprintSyntheticPackage",
                        ":left:generateSyntheticLinkageSwiftPMImportProjectForCinteropsAndLdDump",
                        ":serializeSwiftPMDependenciesMetadataForLockFiles",
                        ":left:serializeSwiftPMDependenciesMetadataForLockFiles",
                        ":generateUmbrellaPackageIdentifierBasedResolutionForDefault",
                        ":fetchUmbrellaPackageIdentifierForDefault",
                        ":left:iosSimulatorArm64ProcessResources",
                        ":left:fetchSyntheticImportProjectPackages",
                        ":left:fingerprintXcodebuildIphonesimulator",
                        ":left:dumpXcodebuildArgsIphonesimulator",
                        ":left:convertSyntheticImportProjectIntoDefFileIphonesimulator",
                        ":left:cinteropSwiftPMImportIosSimulatorArm64",
                        ":left:compileKotlinIosSimulatorArm64",
                        ":left:iosSimulatorArm64MainKlibrary",
                        ":left:compileTestKotlinIosSimulatorArm64",
                        ":left:linkDebugTestIosSimulatorArm64",
                    )
                }
            }
        }
    }
}

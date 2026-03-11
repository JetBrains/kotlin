/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.*
import kotlin.test.*

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)

@DisplayName("SwiftPM import Persistent Package.resolved integration tests")
@NativeGradlePluginTests
class SwiftPMImportPersistentPackageLockIntegrationTests : KGPBaseTest() {

    /**
     * Verifies that resolving remote SwiftPM git packages writes a Package.resolved file at the project root.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `fetchSyntheticImportProjectPackages writes Package_resolved at root when remote git packages are resolved`(version: GradleVersion) {

        project("empty", version) {
            // Our tests recreate the Git repositories from scratch for each run, which produces new commit hashes
            // for the same tag (e.g. "1.0.0"). If we reuse the same package name/URL, SwiftPM may reuse a cached
            // checkout and fail with errors like:
            //
            //   "Revision ... for TestPackageA version 1.0.0 does not match previously recorded value ..."
            //
            // To avoid cache collisions between test runs, we generate a unique package name (and therefore URL)
            // for each execution.
            val repoAName = "TestPackageA_1"
            val repoBName = "TestPackageB_1"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirFile = cacheDir.toFile()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0"),
            )

            createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoBName,
                tags = listOf("1.0.0"),
            )

            val daemon = GitDaemon(reposRoot)

            daemon.useWithFailure {

                daemon.start()
                val repoAUrl = daemon.urlFor(repoAName)
                val repoBUrl = daemon.urlFor(repoBName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirFile)
                        }

                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repoAUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoAName))
                        )
                        swiftPackage(
                            url = url(repoBUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoBName))
                        )
                    }
                }

                assertTrue(!projectPath.resolve("Package.resolved").exists(), "Root Package.resolved should not be generated")

                /**
                 * First run
                 *  - No project directory level Package.resolved
                 *  - No synthetic project Package.resolved
                 */
                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")

                    val resolvedText = resolved.readText()

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.0"
                                    )
                                ),
                                SwiftPmPin(
                                    identity = repoBName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoBUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.0"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )

                    // The synthetic lock file written to project directory, now project directory Package.resolved created.
                    assertTasksExecuted(":syncPackageSwiftLockFileToRoot")
                    // Skip cause no project directory level Package.resolved is generated on the first run, so there's no lock file to sync to the synthetic project
                    assertTasksSkipped(":syncPackageSwiftLockFileToSyntheticProject")
                }
            }
        }
    }

    /**
     * Ensures that when using the `from` version key, the latest compatible version is written to the root Package_resolved.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `fetchSyntheticImportProjectPackages writes latest version to Package_resolved at root when version key is from`(version: GradleVersion) {

        project("empty", version) {

            val repoAName = "TestPackageA_2"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirFile = cacheDir.toFile()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0", "1.0.1"),
            )

            val daemon = GitDaemon(reposRoot)

            daemon.useWithFailure {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirFile)
                        }
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repoAUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoAName))
                        )
                    }
                }

                /**
                 * First run
                 *  - No project directory level Package.resolved
                 *  - No synthetic project Package.resolved
                 */
                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")

                    val resolvedText = resolved.readText()

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.1"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )

                    assertTasksExecuted(":syncPackageSwiftLockFileToRoot")
                    assertTasksSkipped(":syncPackageSwiftLockFileToSyntheticProject")

                }
            }
        }
    }

    /**
     * Verifies incremental behavior with floating versions: a second run reuses the already locked root Package_resolved.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `fetchSyntheticImportProjectPackages reuses Package_resolved at root when incremental run with floating versions`(version: GradleVersion) {

        project("empty", version) {

            val repoAName = "TestPackageA_3"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirFile = cacheDir.toFile()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0"),
            )

            val daemon = GitDaemon(reposRoot)

            daemon.useWithFailure {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirFile)
                        }
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repoAUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoAName))
                        )
                    }
                }

                /**
                 * First run
                 *  - No project directory level Package.resolved
                 *  - No synthetic project Package.resolved
                 */
                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")

                    val resolvedText = resolved.readText()

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.0"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )
                }

                /**
                 * First run
                 *  -  project directory level Package.resolved exists
                 *  -  synthetic project Package.resolved exists
                 */
                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val buildScriptresolved = projectPath.resolve("Package.resolved")
                    val syntheticResolved = projectPath.resolve("build/kotlin/swiftImport/Package.resolved")

                    assertTrue(buildScriptresolved.exists(), "Root Package.resolved should be generated")

                    val resolvedText = buildScriptresolved.readText()
                    val syntheticResolvedText = syntheticResolved.readText()

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)
                    val swiftPmPackageResolvedSynthetic = parsePackageResolved(syntheticResolvedText)


                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.0"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )

                    assertTasksExecuted(":syncPackageSwiftLockFileToSyntheticProject")
                    assertTasksUpToDate(":syncPackageSwiftLockFileToRoot")

                    assertEquals(
                        swiftPmPackageResolved.ignoreRevisions().ignoreTopLevelVersion(),
                        swiftPmPackageResolvedSynthetic.ignoreRevisions().ignoreTopLevelVersion()
                    )
                }
            }
        }
    }

    /**
     * Ensures a newer compatible tag is not picked when a root Package_resolved already locks a version.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `fetchSyntheticImportProjectPackages keeps locked version in Package_resolved at root when new tag is released and root lock file exists`(version: GradleVersion) {

        project("empty", version) {

            val repoAName = "TestPackageA_4"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirFile = cacheDir.toFile()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            val repoA = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0"),
            )

            val daemon = GitDaemon(reposRoot)

            daemon.useWithFailure {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirFile)
                        }
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repoAUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoAName))
                        )
                    }
                }

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")

                    val resolvedText = resolved.readText()

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.0"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )
                }

                addSwiftPmGitTag(
                    repoA,
                    "1.0.1",
                    mapOf(
                        "Sources/${repoAName}/${repoAName}_1_0_1.swift" to
                                "public struct ${repoAName}_1_0_1 { public static let v = \"1_0_1\" }\n"
                    )
                )

                /**
                 * clean the remove build/kotlin/swiftImport directory to force re-resolution
                 * and ensure the root Package_resolved is reused and not overridden with the newer tag
                 */
                build(
                    "clean"
                )

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")

                    val resolvedText = resolved.readText()

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.0"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )

                    assertTasksExecuted(":syncPackageSwiftLockFileToSyntheticProject")
                }
            }
        }
    }


    /**
     * Ensures that if the root Package_resolved is removed after a newer tag is released, the next run locks to the latest tag.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `fetchSyntheticImportProjectPackages writes latest tag to Package_resolved at root when root lock file is removed after newer tag is released`(version: GradleVersion) {

        project("empty", version) {

            val repoAName = "TestPackageA_5"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirFile = cacheDir.toFile()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")
            val repoA = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0"),
            )

            val daemon = GitDaemon(reposRoot)

            daemon.useWithFailure {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirFile)
                        }
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repoAUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoAName))
                        )
                    }
                }

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")

                    val resolvedText = resolved.readText()

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.0"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )
                }

                addSwiftPmGitTag(
                    repoA,
                    "1.0.1",
                    mapOf(
                        "Sources/${repoAName}/${repoAName}_1_0_1.swift" to
                                "public struct ${repoAName}_1_0_1 { public static let v = \"1_0_1\" }\n"
                    )
                )


                /**
                 * No pinned versions anymore, so the root Package_resolved should be deleted to allow picking up the newer tag on the next run.
                 */
                projectPath.resolve("Package.resolved").deleteIfExists()

                build(
                    "clean"
                )

                assertFalse(projectPath.resolve("Package.resolved").exists(), "Root Package.resolved should be deleted")

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should be regenerated")

                    val resolvedText = resolved.readText()

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)

                    val gitCheckoutTag = runGit(
                        "tag", "--points-at", "HEAD",
                        repoDir = projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts/${repoAName}")
                    ).trim()

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.1"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )

                    assertEquals(gitCheckoutTag, "1.0.1", "Root Package.resolved should have the same version as the tag")
                }
            }
        }
    }


    /**
     * Ensures the synthetic project's Package_resolved stays in sync with the root Package_resolved.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `fetchSyntheticImportProjectPackages keeps Package_resolved at synthetic project in sync with Package_resolved at root`(version: GradleVersion) {

        project("empty", version) {

            val repoAName = "TestPackageA_6"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirFile = cacheDir.toFile()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0"),
            )

            val daemon = GitDaemon(reposRoot)

            daemon.useWithFailure {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirFile)
                        }
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repoAUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoAName))
                        )
                    }
                }

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val rootResolved = projectPath.resolve("Package.resolved")
                    val syntheticResolved = projectPath.resolve("build/kotlin/swiftImport/Package.resolved")

                    assertTrue(rootResolved.exists(), "Root Package.resolved should be generated")
                    assertTrue(syntheticResolved.exists(), "Synthetic project Package.resolved should be generated")

                    val rootResolvedText = rootResolved.readText()
                    val syntheticResolvedText = syntheticResolved.readText()

                    val swiftPmPackageResolved = parsePackageResolved(rootResolvedText)

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.0"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )

                    assertEquals(rootResolvedText, syntheticResolvedText, "Root and synthetic Package.resolved should stay in sync")

                    assertTasksSkipped(":syncPackageSwiftLockFileToSyntheticProject")
                    assertTasksExecuted(":syncPackageSwiftLockFileToRoot")
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

            val useFromVersionKey = "useFromVersionKey"

            val repoAName = "TestPackageA_7"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirFile = cacheDir.toFile()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0", "1.0.1"),
            )

            val daemon = GitDaemon(reposRoot)

            daemon.useWithFailure {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirFile)
                        }
                    if (project.hasProperty(useFromVersionKey)) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(repoAUrl),
                                version = from("1.0.0"),
                                products = listOf(product(repoAName))
                            )
                        }
                    } else {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(repoAUrl),
                                version = exact("1.0.0"),
                                products = listOf(product(repoAName))
                            )
                        }
                    }
                }

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")

                    val resolvedText = resolved.readText()

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.0"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )
                    assertTasksSkipped(":syncPackageSwiftLockFileToSyntheticProject")
                    assertTasksExecuted(":syncPackageSwiftLockFileToRoot")
                }

                build(
                    "clean"
                )

                build(
                    "fetchSyntheticImportProjectPackages",
                    "-P${useFromVersionKey}=true"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")

                    val resolvedText = resolved.readText()

                    val gitCheckoutTag = runGit(
                        "tag", "--points-at", "HEAD",
                        repoDir = projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts/${repoAName}")
                    ).trim()

                    assertTasksExecuted(":syncPackageSwiftLockFileToSyntheticProject")

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.0"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )
                    assertEquals(gitCheckoutTag, "1.0.0", "The tag should be checked out")
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

            val useFromVersionKey = "useFromVersionKey"

            val repoAName = "TestPackageA_8"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirFile = cacheDir.toFile()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0", "1.0.1"),
            )

            val daemon = GitDaemon(reposRoot)

            daemon.useWithFailure {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirFile)
                        }
                    if (!project.hasProperty(useFromVersionKey)) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(repoAUrl),
                                version = from("1.0.0"),
                                products = listOf(product(repoAName))
                            )
                        }
                    } else {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(repoAUrl),
                                version = exact("1.0.0"),
                                products = listOf(product(repoAName))
                            )
                        }
                    }
                }

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")

                    val resolvedText = resolved.readText()

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.1"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )
                    assertTasksSkipped(":syncPackageSwiftLockFileToSyntheticProject")
                    assertTasksExecuted(":syncPackageSwiftLockFileToRoot")
                }

                build(
                    "clean"
                )

                build(
                    "fetchSyntheticImportProjectPackages",
                    "-P${useFromVersionKey}=true"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")

                    val resolvedText = resolved.readText()

                    val gitCheckoutTag = runGit(
                        "tag", "--points-at", "HEAD",
                        repoDir = projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts/${repoAName}")
                    ).trim()

                    assertTasksExecuted(":syncPackageSwiftLockFileToSyntheticProject")

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.0"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )
                    assertEquals(gitCheckoutTag, "1.0.0", "The tag should be checked out")
                }
            }
        }
    }

    /**
     * Ensures cinterop task respects the existing root lock: cleaning build does not change locked versions when root Package_resolved exists.
     */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    fun `cinteropSwiftPMImportIosArm64 keeps root lock semantics when build directory is removed and Package_resolved at root still exists`(version: GradleVersion) {

        project("empty", version) {

            val repoAName = "TestPackageA_9"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirFile = cacheDir.toFile()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            val repoA = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0"),
            )

            val daemon = GitDaemon(reposRoot)

            daemon.useWithFailure {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirFile)
                        }
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repoAUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoAName))
                        )
                    }
                }

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")

                    val resolvedText = resolved.readText()

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.0"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )
                }

                addSwiftPmGitTag(
                    repoA,
                    "1.0.1",
                    mapOf(
                        "Sources/${repoAName}/${repoAName}_1_0_1.swift" to
                                "public struct ${repoAName}_1_0_1 { public static let v = \"1.0.1\" }\n"
                    )
                )

                build(
                    "clean"
                )

                assertTrue(projectPath.resolve("Package.resolved").exists(), "Root Package.resolved should still exist")

                build(
                    "cinteropSwiftPMImportIosArm64"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    val resolvedText = resolved.readText()

                    val swiftPmPackageResolved = parsePackageResolved(resolvedText)

                    val gitCheckoutTag = runGit(
                        "tag", "--points-at", "HEAD",
                        repoDir = projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts/${repoAName}")
                    ).trim()

                    assertEquals(
                        SwiftPmPackageResolved(
                            listOf(
                                SwiftPmPin(
                                    identity = repoAName.lowercase(),
                                    kind = "remoteSourceControl",
                                    location = repoAUrl,
                                    SwiftPmPinState(
                                        revision = "<ignored>",
                                        version = "1.0.0"
                                    )
                                )
                            ), version = -1
                        ),
                        swiftPmPackageResolved
                            .ignoreRevisions()
                            .ignoreTopLevelVersion()
                    )

                    assertEquals(gitCheckoutTag, "1.0.0", "Root Package.resolved should still have the same version as the tag")

                }
            }
        }
    }
}




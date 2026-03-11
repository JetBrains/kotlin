/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
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

@OptIn(EnvironmentalVariablesOverride::class)
@DisplayName("SwiftPM import Persistent Package.resolved integration tests")
@NativeGradlePluginTests
class SwiftPMImportPersistentPackageLockIntegrationTests : KGPBaseTest() {

    /**
     * Verifies that resolving remote SwiftPM git packages writes a Package.resolved file at the project root.
     */
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
            val repoAName = "TestPackageA_${System.currentTimeMillis()}"
            val repoBName = "TestPackageB_${System.currentTimeMillis()}"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirAbsolutePath = cacheDir.toAbsolutePath().toString()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")
            val repoA = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0"),
            )
            val repoB = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoBName,
                tags = listOf("1.0.0"),
            )

            val daemon = GitDaemon(reposRoot)

            try {
                daemon.start()
                val repoAUrl = daemon.urlFor(repoAName)
                val repoBUrl = daemon.urlFor(repoBName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirAbsolutePath)
                        }

                    swiftPMDependencies {
                        `package`(
                            url = url(repoAUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoAName))
                        )
                        `package`(
                            url = url(repoBUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoBName))
                        )
                    }
                }

                assertTrue(!projectPath.resolve("Package.resolved").exists(), "Root Package.resolved should not be generated")

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")

                    val resolvedText = resolved.readText()

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.0")
                    assertPackageResolvedContains(resolvedText, "TestPackageB", repoBUrl, "1.0.0")

                    assertTasksExecuted(":syncPackageSwiftLockFileToRoot")
                    assertTasksExecuted(":syncPackageSwiftLockFileToSyntheticProject")
                }
            } finally {
                daemon.close()
                reposRoot.toFile().deleteRecursively()
                repoA.toFile().deleteRecursively()
                repoB.toFile().deleteRecursively()
                cacheDir.deleteRecursively()
            }
        }
    }

    /**
     * Ensures that when using the `from` version key, the latest compatible version is written to the root Package_resolved.
     */
    @GradleTest
    fun `fetchSyntheticImportProjectPackages writes latest version to Package_resolved at root when version key is from`(version: GradleVersion) {

        project("empty", version) {

            val repoAName = "TestPackageA_${System.currentTimeMillis()}"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirAbsolutePath = cacheDir.toAbsolutePath().toString()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")
            val repoA = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0", "1.0.1"),
            )

            val daemon = GitDaemon(reposRoot)

            try {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirAbsolutePath)
                        }

                    swiftPMDependencies {
                        `package`(
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
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")

                    val resolvedText = resolved.readText()

                    assertTasksExecuted(":syncPackageSwiftLockFileToRoot")
                    assertTasksExecuted(":syncPackageSwiftLockFileToSyntheticProject")

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.1")
                }
            } finally {
                daemon.close()
                cacheDir.deleteRecursively()
                reposRoot.toFile().deleteRecursively()
                repoA.toFile().deleteRecursively()
            }
        }
    }

    /**
     * Verifies incremental behavior with floating versions: a second run reuses the already locked root Package_resolved.
     */
    @GradleTest
    fun `fetchSyntheticImportProjectPackages reuses Package_resolved at root when incremental run with floating versions`(version: GradleVersion) {

        project("empty", version) {

            val repoAName = "TestPackageA_${System.currentTimeMillis()}"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirAbsolutePath = cacheDir.toAbsolutePath().toString()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            val repoA = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0"),
            )

            val daemon = GitDaemon(reposRoot)

            try {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirAbsolutePath)
                        }

                    swiftPMDependencies {
                        `package`(
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
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")
                    val resolvedText = resolved.readText()

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.0")
                }

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")
                    val resolvedText = resolved.readText()

                    assertContains(resolvedText, "TestPackageA")
                    assertContains(resolvedText, "\"location\" : \"$repoAUrl\"")
                    assertContains(resolvedText, "\"version\" : \"1.0.0\"")

                    assertTasksExecuted(":syncPackageSwiftLockFileToSyntheticProject")
                }

            } finally {
                daemon.close()
                cacheDir.deleteRecursively()
                reposRoot.toFile().deleteRecursively()
                repoA.toFile().deleteRecursively()
            }
        }
    }

    /**
     * Ensures a newer compatible tag is not picked when a root Package_resolved already locks a version.
     */
    @GradleTest
    fun `fetchSyntheticImportProjectPackages keeps locked version in Package_resolved at root when new tag is released and root lock file exists`(version: GradleVersion) {

        project("empty", version) {

            val repoAName = "TestPackageA_${System.currentTimeMillis()}"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirAbsolutePath = cacheDir.toAbsolutePath().toString()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")
            val repoA = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0"),
            )

            val daemon = GitDaemon(reposRoot)

            try {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirAbsolutePath)
                        }

                    swiftPMDependencies {
                        `package`(
                            url = url(repoAUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoAName))
                        )
                    }
                }

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolvedText = projectPath.resolve("Package.resolved").readText()
                    assertPackageResolvedContains(resolvedText, null, repoAUrl, "1.0.0")
                }

                addSwiftPmGitTag(
                    repoA,
                    repoAName,
                    "1.0.1",
                    mapOf(
                        "Sources/${repoAName}/${repoAName}_1_0_1.swift" to
                                "public struct ${repoAName}_1_0_1 { public static let v = \"1_0_1\" }\n"
                    )
                )

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")
                    val resolvedText = resolved.readText()

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.0")
                }
            } finally {
                daemon.close()
                cacheDir.deleteRecursively()
                reposRoot.toFile().deleteRecursively()
                repoA.toFile().deleteRecursively()
            }
        }
    }


    /**
     * Ensures that if the root Package_resolved is removed after a newer tag is released, the next run locks to the latest tag.
     */
    @GradleTest
    fun `fetchSyntheticImportProjectPackages writes latest tag to Package_resolved at root when root lock file is removed after newer tag is released`(version: GradleVersion) {

        project("empty", version) {

            val repoAName = "TestPackageA_${System.currentTimeMillis()}"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirAbsolutePath = cacheDir.toAbsolutePath().toString()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")
            val repoA = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0"),
            )

            val daemon = GitDaemon(reposRoot)

            try {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirAbsolutePath)
                        }

                    swiftPMDependencies {
                        `package`(
                            url = url(repoAUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoAName))
                        )
                    }
                }

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolvedText = projectPath.resolve("Package.resolved").readText()

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.0")
                }

                addSwiftPmGitTag(
                    repoA,
                    repoAName,
                    "1.0.1",
                    mapOf(
                        "Sources/${repoAName}/${repoAName}_1_0_1.swift" to
                                "public struct ${repoAName}_1_0_1 { public static let v = \"1_0_1\" }\n"
                    )
                )

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

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.1")
                }
            } finally {
                daemon.close()
                cacheDir.deleteRecursively()
                reposRoot.toFile().deleteRecursively()
                repoA.toFile().deleteRecursively()
            }
        }
    }

    /**
     * Ensures that deleting the build directory and the root Package_resolved forces resolution to update and lock to the latest tag.
     */
    @GradleTest
    fun `fetchSyntheticImportProjectPackages updates Package_resolved at root when build directory and root Package_resolved are removed`(version: GradleVersion) {

        project("empty", version) {

            val repoAName = "TestPackageA_${System.currentTimeMillis()}"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirAbsolutePath = cacheDir.toAbsolutePath().toString()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            val repoA = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0"),
            )

            val daemon = GitDaemon(reposRoot)

            try {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirAbsolutePath)
                        }

                    swiftPMDependencies {
                        `package`(
                            url = url(repoAUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoAName))
                        )
                    }
                }

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolvedText = projectPath.resolve("Package.resolved").readText()

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.0")

                }

                addSwiftPmGitTag(
                    repoA,
                    repoAName,
                    "1.0.1",
                    mapOf(
                        "Sources/${repoAName}/${repoAName}_1_0_1.swift" to
                                "public struct ${repoAName}_1_0_1 { public static let v = \"1.0.1\" }\n"
                    )
                )

                build(
                    "clean"
                )

                projectPath.resolve("Package.resolved").deleteIfExists()

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should still exist after rerun")
                    val resolvedText = resolved.readText()

                    val gitCheckoutTag = runGit(
                        "tag", "--points-at", "HEAD",
                        repoDir = projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts/${repoAName}")
                    ).trim()

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.1")

                    assertEquals(gitCheckoutTag, "1.0.1", "Root Package.resolved should still have the same version as the tag")
                }
            } finally {
                daemon.close()
                cacheDir.deleteRecursively()
                reposRoot.toFile().deleteRecursively()
                repoA.toFile().deleteRecursively()
            }
        }
    }


    /**
     * Ensures the synthetic project's Package_resolved stays in sync with the root Package_resolved.
     */
    @GradleTest
    fun `fetchSyntheticImportProjectPackages keeps Package_resolved at synthetic project in sync with Package_resolved at root`(version: GradleVersion) {

        project("empty", version) {

            val repoAName = "TestPackageA_${System.currentTimeMillis()}"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirAbsolutePath = cacheDir.toAbsolutePath().toString()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            val repoA = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0"),
            )

            val daemon = GitDaemon(reposRoot)

            try {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirAbsolutePath)
                        }
                    swiftPMDependencies {
                        `package`(
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

                    assertPackageResolvedContains(rootResolvedText, "TestPackageA", repoAUrl, "1.0.0")

                    assertEquals(rootResolvedText, syntheticResolvedText, "Root and synthetic Package.resolved should stay in sync")

                    assertTasksExecuted(":syncPackageSwiftLockFileToSyntheticProject")
                    assertTasksExecuted(":syncPackageSwiftLockFileToRoot")
                }
            } finally {
                daemon.close()
                cacheDir.deleteRecursively()
                reposRoot.toFile().deleteRecursively()
                repoA.toFile().deleteRecursively()
            }
        }
    }

    /**
     * Verifies that changing an exact version to a newer exact version is reflected after deleting the lock file.
     */
    @GradleTest
    fun `fetchSyntheticImportProjectPackages updates Package_resolved at root when dependency changes from exact to newer exact version and lock file is removed`(version: GradleVersion) {

        project("empty", version) {

            val useNewExactVersionArgumentName = "useNewExactVersion"

            val repoAName = "TestPackageA_${System.currentTimeMillis()}"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirAbsolutePath = cacheDir.toAbsolutePath().toString()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            val repoA = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0", "1.0.1"),
            )

            val daemon = GitDaemon(reposRoot)

            try {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirAbsolutePath)
                        }

                    val repoAVersion =
                        if (project.hasProperty(useNewExactVersionArgumentName)) "1.0.1" else "1.0.0"

                    swiftPMDependencies {
                        `package`(
                            url = url(repoAUrl),
                            version = exact(repoAVersion),
                            products = listOf(product(repoAName))
                        )
                    }
                }

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolvedText = projectPath.resolve("Package.resolved").readText()

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.0")
                }

                projectPath.resolve("Package.resolved").deleteIfExists()

                build(
                    "fetchSyntheticImportProjectPackages",
                    "-P${useNewExactVersionArgumentName}=true"
                ) {
                    val resolved = projectPath.resolve("Package.resolved")
                    assertTrue(resolved.exists(), "Root Package.resolved should be generated")
                    val resolvedText = resolved.readText()

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.1")
                }
            } finally {
                daemon.close()
                cacheDir.deleteRecursively()
                reposRoot.toFile().deleteRecursively()
                repoA.toFile().deleteRecursively()
            }
        }
    }


    /**
     * Verifies that switching the version key from exact to from does not change the locked versions in Package_resolved.
     */
    @GradleTest
    fun `fetchSyntheticImportProjectPackages locks versions in Package_resolved when version key changes from exact to from`(version: GradleVersion) {

        project("empty", version) {

            val useFromVersionKey = "useFromVersionKey"

            val repoAName = "TestPackageA_${System.currentTimeMillis()}"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirAbsolutePath = cacheDir.toAbsolutePath().toString()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            val repoA = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0", "1.0.1"),
            )

            val daemon = GitDaemon(reposRoot)

            try {
                daemon.start()
                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirAbsolutePath)
                        }

                    if (project.hasProperty(useFromVersionKey)) {
                        swiftPMDependencies {
                            `package`(
                                url = url(repoAUrl),
                                version = from("1.0.0"),
                                products = listOf(product(repoAName))
                            )
                        }
                    } else {
                        swiftPMDependencies {
                            `package`(
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

                    assertTasksExecuted(":syncPackageSwiftLockFileToSyntheticProject")
                    assertTasksExecuted(":syncPackageSwiftLockFileToRoot")

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.0")
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

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.0")

                    assertEquals(gitCheckoutTag, "1.0.0", "The tag should be checked out")
                }
            } finally {
                daemon.close()
                cacheDir.deleteRecursively()
                reposRoot.toFile().deleteRecursively()
                repoA.toFile().deleteRecursively()
            }
        }
    }

    /**
     * Ensures cinterop task respects the existing root lock: cleaning build does not change locked versions when root Package_resolved exists.
     */
    @GradleTest
    fun `cinteropSwiftPMImportIosArm64 keeps root lock semantics when build directory is removed and Package_resolved at root still exists`(version: GradleVersion) {

        project("empty", version) {

            val repoAName = "TestPackageA_${System.currentTimeMillis()}"

            val cacheDir = projectPath.resolve("customXcodePackageCache")
            val cacheDirAbsolutePath = cacheDir.toAbsolutePath().toString()

            val reposRoot = createTempDirectory(projectPath, "spmRepos")

            val repoA = createSwiftPmGitRepoWithTags(
                reposRoot = reposRoot,
                packageName = repoAName,
                tags = listOf("1.0.0"),
            )

            val daemon = GitDaemon(reposRoot)

            try {

                daemon.start()

                val repoAUrl = daemon.urlFor(repoAName)

                initDefaultKmp {

                    project.tasks
                        .withType(FetchSyntheticImportProjectPackages::class.java)
                        .configureEach { task ->
                            task.xcodePackageCacheDir.set(cacheDirAbsolutePath)
                        }

                    swiftPMDependencies {
                        `package`(
                            url = url(repoAUrl),
                            version = from("1.0.0"),
                            products = listOf(product(repoAName))
                        )
                    }
                }

                build(
                    "fetchSyntheticImportProjectPackages"
                ) {
                    val resolvedText = projectPath.resolve("Package.resolved").readText()

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.0")
                }

                addSwiftPmGitTag(
                    repoA,
                    repoAName,
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

                    val gitCheckoutTag = runGit(
                        "tag", "--points-at", "HEAD",
                        repoDir = projectPath.resolve("build/kotlin/swiftPMCheckout/checkouts/${repoAName}")
                    ).trim()

                    assertPackageResolvedContains(resolvedText, "TestPackageA", repoAUrl, "1.0.0")

                    assertEquals(gitCheckoutTag, "1.0.0", "Root Package.resolved should still have the same version as the tag")
                }
            } finally {
                daemon.close()
                cacheDir.deleteRecursively()
                reposRoot.toFile().deleteRecursively()
                repoA.toFile().deleteRecursively()
            }
        }
    }
}




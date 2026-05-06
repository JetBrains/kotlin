/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle.targets.web

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.web.KotlinNpmToolingLockFilesTest.Companion.packageLockJson
import org.jetbrains.kotlin.gradle.targets.web.KotlinNpmToolingLockFilesTest.Companion.packageLockJsonContent
import org.jetbrains.kotlin.gradle.targets.web.KotlinNpmToolingLockFilesTest.Companion.yarnLockContent
import org.jetbrains.kotlin.gradle.testing.js.PackageJson
import org.jetbrains.kotlin.gradle.testing.js.PackageLockJson
import org.jetbrains.kotlin.gradle.testing.js.YarnLock
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.assertEquals

class KotlinNpmToolingLockFilesTest {

    /**
     * Sanity check to make sure `package-lock.json` was regenerated after a change in `package.json`.
     */
    @Test
    fun `expect package-json and package-lock-json contain same dependencies`() {
        assertEquals(
            packageJson.dependencies.prettyPrinted,
            packageLockJsonRootPackage.dependencies.prettyPrinted,
        )
        assertEquals(
            packageJson.devDependencies.prettyPrinted,
            packageLockJsonRootPackage.devDependencies.prettyPrinted,
        )
        assertEquals(
            packageJson.peerDependencies.prettyPrinted,
            packageLockJsonRootPackage.peerDependencies.prettyPrinted,
        )
        assertEquals(
            packageJson.optionalDependencies.prettyPrinted,
            packageLockJsonRootPackage.optionalDependencies.prettyPrinted,
        )
        assertEquals(
            packageJson.bundledDependencies.prettyPrinted,
            packageLockJsonRootPackage.bundledDependencies.prettyPrinted,
        )
    }

    /**
     * Sanity check to make sure `yarn.lock` was regenerated after a change in `package.json`.
     */
    @Test
    fun `expect package-json and yarn-lock contain same dependencies`() {
        val allPackageJsonDependencies =
            buildList {
                addAll(packageJson.dependencies.entries)
                addAll(packageJson.devDependencies.entries)
                addAll(packageJson.peerDependencies.entries)
                addAll(packageJson.optionalDependencies.entries)
            }
                .map { (dep, ver) -> "$dep@$ver" }
                .distinct()
                .sorted()

        val allYarnLockDeps =
            yarnLock.entries.flatMap {
                it.versions.map { ver -> "${it.name}@$ver" }
            }.toSet()

        val missing = allPackageJsonDependencies.filter { it !in allYarnLockDeps }

        assertTrue(missing.isEmpty(), "Missing dependencies in yarn.lock: $missing")
    }

    /**
     * The npm `package-lock.json` file must not have a version.
     *
     * ```json
     * {
     *   "name": "kotlin-npm-tooling",
     *   "packages": {
     *     "": {
     *       "name": "kotlin-npm-tooling",
     *       "version": "...", // forbidden!
     *       "dependencies": { /* ... */ }
     * ```
     *
     * The lockfile is used in a shared directory, which can be used by multiple projects
     * with differing KGP versions.
     * To prevent the `package-lock.json` from changing unnecessarily
     * it should not contain irrelevant data, like the version.
     *
     * There's no need to check `yarn.lock` - it doesn't contain a project version.
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinToolingSetupTask
     */
    @Test
    fun `verify npm lockfile does not have project version`() {
        assertNull(packageLockJsonRootPackage.version)
    }

    @Test
    fun `npm lockfile should contain all dependencies in NpmVersions`() {
        assertLockfileContainsNpmVersionsDependencies(
            lockfileDependencies = packageLockJsonPackages,
            lockfileName = "package-lock.json",
        )
    }

    @Test
    fun `yarn lockfile should contain all dependencies in NpmVersions`() {
        assertLockfileContainsNpmVersionsDependencies(
            lockfileDependencies = yarnLockPackages,
            lockfileName = "yarn.lock",
        )
    }

    private fun assertLockfileContainsNpmVersionsDependencies(
        lockfileDependencies: List<String>,
        lockfileName: String,
    ) {
        val allNpmVersionsDependencies = NpmVersions().allDependencies.map { it.name }

        val missingElements = allNpmVersionsDependencies
            .filter { it !in lockfileDependencies }

        assertTrue(missingElements.isEmpty()) {
            buildString {
                appendLine("Missing ${missingElements.size} dependencies in $lockfileName:")
                appendLine(missingElements)
                appendLine("NpmVersions dependencies: $allNpmVersionsDependencies")
                appendLine("All dependencies in $lockfileName: $lockfileDependencies")
            }
        }
    }

    /**
     * Check all dependencies in [NpmVersions] are present in [packageLockJson],
     * with the expected version.
     */
    @Test
    fun `expect all dependencies in NpmVersions contain the resolved versions in npm lockfile`() {
        val mapActualToExpected =
            NpmVersions().allDependencies
                // I don't know if there's a sensible way to check github versions?
                .filter { !it.version.startsWith("github:") }
                .associate { d ->
                    val actual = "${d.name} : ${d.version}"
                    val expected = packageLockJson.packages["node_modules/${d.name}"]?.let { pkg ->
                        "${d.name} : ${pkg.version}"
                    }
                    actual to expected
                }

        val mismatches = mapActualToExpected.filter { it.key != it.value }

        assertTrue(mismatches.isEmpty()) {
            buildString {
                appendLine("Found mismatches between NpmVersions and package-lock.json:")
                append(mismatches.entries.joinToString("\n") { (actual, expected) -> "$actual != $expected" })
            }
        }
    }

    /**
     * Load `package-lock.json` and `yarn.lock` files and check all dependencies are the same
     * (ignoring version).
     */
    @Test
    fun `verify yarn and npm lockfiles have same dependencies`() {
        assertEquals(
            yarnLockPackages.prettyPrinted,
            packageLockJsonPackages.prettyPrinted,
        )
    }

    companion object {

        /** `package.json` for KGP's tooling dependencies. */
        private val packageJson: PackageJson by lazy {
            val file = System.getProperty("kgpNpmToolingPackageJson") ?: error("missing kgpNpmToolingPackageJson system property")
            val content = Path(file).readText()
            json.decodeFromString(PackageJson.serializer(), content)
        }

        /** `package-lock.json` file for KGP's tooling dependencies. */
        private val packageLockJsonContent: String by lazy {
            loadResource("/org/jetbrains/kotlin/gradle/targets/js/npm/package-lock.json")
        }

        /** Parsed [packageLockJsonContent]. */
        private val packageLockJson: PackageLockJson by lazy {
            json.decodeFromString(PackageLockJson.serializer(), packageLockJsonContent)
        }

        private val packageLockJsonRootPackage: PackageLockJson.Package by lazy {
            val rootPackage = packageLockJson.packages[""]
            requireNotNull(rootPackage) {
                "Missing root package in package-lock.json. All packages: ${packageLockJson.packages.keys}"
            }
        }

        /** `yarn.lock` file for KGP's tooling dependencies. */
        private val yarnLockContent: String by lazy {
            loadResource("/org/jetbrains/kotlin/gradle/targets/js/yarn/yarn.lock")
        }

        /** `yarn.lock` file for KGP's tooling dependencies. */
        private val yarnLock: YarnLock by lazy {
            YarnLock.decodeFrom(yarnLockContent)
        }

        /** All (non-nested) dependencies in [packageLockJson]. */
        private val packageLockJsonPackages: List<String> by lazy {
            packageLockJson.packages.keys
                .asSequence()
                .filter { it.startsWith("node_modules/") }
                .filter { "/node_modules/" !in it }
                .map { it.substringAfter("node_modules/") }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
                .toList()
        }

        /** All dependencies in [yarnLockContent]. */
        private val yarnLockPackages: List<String> by lazy {
            yarnLock.entries
                .map { it.name }
                .distinct()
                .sorted()
                .toList()
        }

        private fun loadResource(path: String): String {
            this::class.java.getResourceAsStream(path).use { source ->
                requireNotNull(source) { "Resource not found: $path" }
                return source.bufferedReader().readText()
            }
        }

        private val json: Json = Json {
            ignoreUnknownKeys = true
        }
    }
}

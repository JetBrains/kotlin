/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.GradleException
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class GradleNodeModuleBuilderTest {

    /**
     * Verify Gson (used in [fromSrcPackageJson]) deserializes JSON to [PackageJson] no matter on nullability and default values.
     *
     * Check that if there are no dependencies, we don't get nullable fields that are declared as non-nullable.
     */
    @Test
    fun validPackageJsonWithoutDependencies(
        @TempDir
        tempDir: File,
    ) {
        val packageJsonFile = tempDir.resolve("package.json")

        packageJsonFile.writeText(
            """
            {
              "name": "npm",
              "version": "1.0.0"
            }
            """.trimIndent()
        )

        val packageJson = fromSrcPackageJson(packageJsonFile)
        assertNotNull(packageJson, "package.json should be deserialized")

        with(packageJson) {
            listOf(
                dependencies,
                devDependencies,
                peerDependencies,
                optionalDependencies,
                bundledDependencies
            ).forEach {
                assertNotNull(it, "Dependencies should deserialized correctly without null")
            }
        }
    }

    /** KT-89245: a `name`/`version` from a dependency's `package.json` must not escape the cache. */
    @Test
    fun importedPackageDirRejectsTraversalNames(
        @TempDir
        tempDir: File,
    ) {
        val cacheDir = tempDir.resolve("packages_imported").apply { mkdirs() }

        assertFailsWith<GradleException>("relative traversal name must be rejected") {
            importedPackageDirWithinCache(cacheDir, "../../evil", "1.0.0")
        }
        assertFailsWith<GradleException>("deep relative traversal name must be rejected") {
            importedPackageDirWithinCache(cacheDir, "../../../../../../../../tmp/evil", "1.0.0")
        }
        assertFailsWith<GradleException>("absolute name must be rejected") {
            importedPackageDirWithinCache(cacheDir, tempDir.resolve("abs-evil").absolutePath, "1.0.0")
        }
        assertFailsWith<GradleException>("sibling-prefix name must be rejected") {
            importedPackageDirWithinCache(cacheDir, "../packages_imported-evil", "1.0.0")
        }
        assertFailsWith<GradleException>("parent-directory name must be rejected") {
            importedPackageDirWithinCache(cacheDir, "..", "1.0.0")
        }
        assertFailsWith<GradleException>("version resolving onto the cache root must be rejected") {
            importedPackageDirWithinCache(cacheDir, "evil", "..")
        }
        // inside the cache, so the name pattern is the only thing rejecting it
        assertFailsWith<GradleException>("nested name must be rejected") {
            importedPackageDirWithinCache(cacheDir, "a/b/c", "1.0.0")
        }
    }

    /** KT-89245: plain and npm-scoped names still resolve inside the cache. */
    @Test
    fun importedPackageDirAcceptsLegitimateNames(
        @TempDir
        tempDir: File,
    ) {
        val cacheDir = tempDir.resolve("packages_imported").apply { mkdirs() }

        val plain = importedPackageDirWithinCache(cacheDir, "my-package", "1.0.0")
        assertEquals(cacheDir.resolve("my-package").resolve("1.0.0"), plain)

        val scoped = importedPackageDirWithinCache(cacheDir, "@scope/my-package", "1.0.0")
        assertEquals(cacheDir.resolve("@scope/my-package").resolve("1.0.0"), scoped)

        val withBuildMetadata = importedPackageDirWithinCache(cacheDir, "my-package", "1.0.0+sha.0a1b2c")
        assertEquals(cacheDir.resolve("my-package").resolve("1.0.0+sha.0a1b2c"), withBuildMetadata)
    }

    /** KT-89245: [makeNodeModule] is public API the reported exploit calls directly, so guard it too. */
    @Suppress("DEPRECATION")
    @Test
    fun makeNodeModuleRejectsTraversalName(
        @TempDir
        tempDir: File,
    ) {
        val cacheDir = tempDir.resolve("packages_imported").apply { mkdirs() }

        assertFailsWith<GradleException> {
            makeNodeModule(cacheDir, PackageJson("../evil", "1.0.0")) { }
        }
    }
}

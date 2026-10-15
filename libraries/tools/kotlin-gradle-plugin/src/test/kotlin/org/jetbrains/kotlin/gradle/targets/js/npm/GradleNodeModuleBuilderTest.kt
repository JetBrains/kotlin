/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
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

    @Test
    fun `resolveChild throws exception when given blank name`(@TempDir tempDir: File) =
        assertResolveChildRejects(tempDir, "", " ", "\t")

    @Test
    fun `resolveChild throws exception when given relative path`(@TempDir tempDir: File) =
        assertResolveChildRejects(tempDir, "..", "../x", "../../evil", ".", "x/..")

    @Test
    fun `resolveChild throws exception when given nested path`(@TempDir tempDir: File) =
        assertResolveChildRejects(tempDir, "a/b")

    @Test
    fun `resolveChild throws exception when given a path that normalizes to a plain path`(@TempDir tempDir: File) =
        assertResolveChildRejects(tempDir, "./a", "a/", "x/../y")

    @Test
    fun `resolveChild throws exception when given absolute path`(@TempDir tempDir: File) =
        assertResolveChildRejects(tempDir, tempDir.parentFile.resolve("escaped").absolutePath)

    @Test
    fun `importedPackageDirWithinCache throws exception when given scoped name with traversal`(@TempDir tempDir: File) {
        assertAllRejected("@x/..", "@../..", "@scope/a/b") { name ->
            importedPackageDirWithinCache(tempDir, name, "1.0.0")
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `makeNodeModule throws exception when given traversal name`(@TempDir tempDir: File) {
        val cacheDir = tempDir.resolve("packages_imported").apply { mkdirs() }

        assertFailsWith<IllegalArgumentException> {
            makeNodeModule(cacheDir, PackageJson("../evil", "1.0.0")) { }
        }
    }

    companion object {
        private fun assertResolveChildRejects(tempDir: File, vararg segments: String) =
            assertAllRejected(*segments) { segment -> tempDir.resolveChild(segment) }

        /** Checks every input, so one that isn't rejected doesn't hide the others. */
        private fun assertAllRejected(vararg inputs: String, action: (String) -> Unit) =
            assertAll(
                inputs.map { input ->
                    {
                        assertFailsWith<IllegalArgumentException>("'$input' must be rejected") { action(input) }
                    }
                })
    }
}

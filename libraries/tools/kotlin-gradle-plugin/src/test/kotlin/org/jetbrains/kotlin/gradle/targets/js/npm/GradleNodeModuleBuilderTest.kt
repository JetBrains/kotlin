/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
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
}

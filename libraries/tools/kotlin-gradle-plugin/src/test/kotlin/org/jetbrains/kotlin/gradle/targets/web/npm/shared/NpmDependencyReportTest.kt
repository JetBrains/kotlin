/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NpmDependencyReportTest {

    @Test
    fun `versions of one npm package are joined into one range`() {
        val report = createNpmDependencyReport(
            dependencies = listOf(
                NpmDependencyEntry("is-even", KotlinNpmDependency.DependencyType.NORMAL, "1.0.0"),
                NpmDependencyEntry("is-even", KotlinNpmDependency.DependencyType.NORMAL, ">1.0.0"),
            ),
            target = "js",
            compilation = "test",
        )

        assertEquals(mapOf("is-even" to "1.0.0 >1.0.0"), report.dependencies)
    }

    @Test
    @Disabled("KT-87942 decide how to handle ||")
    fun `versions with logical-or are handled`() {
        assertEquals("TODO - either display informative error, or merge them somehow", mergedVersionSpec("1.0.0 || 2.0.0", ">=1.5.0"))
    }

    @Test
    @Disabled("KT-87942 decide how to handle hyphen ranges")
    fun `versions with hyphen are handled`() {
        assertEquals("TODO - either display informative error, or merge them somehow", mergedVersionSpec("1.0.0 - 2.0.0", ">=1.5.0"))
    }

    @Test
    @Disabled("KT-87942")
    fun `different local paths cannot be joined`() {
        assertThrows<IllegalArgumentException> { mergedVersionSpec("file:/libs/is-even-1", "file:/libs/is-even-2") }
        assertThrows<IllegalArgumentException> { mergedVersionSpec("file:/libs/is-even", "1.0.0") }
        assertThrows<IllegalArgumentException> { mergedVersionSpec("github:example/is-even", "1.0.0") }
        assertThrows<IllegalArgumentException> { mergedVersionSpec("npm:is-odd@1.0.0", "1.0.0") }
        assertThrows<IllegalArgumentException> { mergedVersionSpec("https://registry.npmjs.org/is-even/-/is-even-1.0.0.tgz", "1.0.0") }
        // TODO what else should be handled?
        //      See all options: https://github.com/npm/node-semver
    }

    /** The merged version spec of one `is-even` package, declared with each of [versionSpecs]. */
    private fun mergedVersionSpec(vararg versionSpecs: String): String? =
        createNpmDependencyReport(
            dependencies = versionSpecs.map { NpmDependencyEntry("is-even", KotlinNpmDependency.DependencyType.NORMAL, it) },
            target = "js",
            compilation = "test",
        ).dependencies["is-even"]
}

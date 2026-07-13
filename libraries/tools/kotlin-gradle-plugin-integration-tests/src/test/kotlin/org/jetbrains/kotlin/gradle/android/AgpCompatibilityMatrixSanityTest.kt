/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.jetbrains.kotlin.gradle.testbase.AndroidGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import kotlin.test.assertTrue

@AndroidGradlePluginTests
class AgpCompatibilityMatrixSanityTest {

    @Test
    fun everyAgpVersionHasCompatibilityMatrixEntry() {
        val matrixVersions = TestVersions.AgpCompatibilityMatrix.entries.map { it.version }.toSet()

        // MIN_SUPPORTED / MAX_SUPPORTED are aliases to concrete AGP_* constants, so they are
        // already covered by the concrete entries and would only add noise here.
        val aliasFields = setOf("MIN_SUPPORTED", "MAX_SUPPORTED")
        val agpVersions = TestVersions.AGP::class.java.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .filter { it.name !in aliasFields }
            .associate { field ->
                field.isAccessible = true
                field.name to (field.get(null) as String)
            }

        val missing = agpVersions.filterValues { it !in matrixVersions }
        assertTrue(
            missing.isEmpty(),
            "Every TestVersions.AGP version must have a corresponding entry in AgpCompatibilityMatrix, " +
                    "otherwise AgpCompatibilityMatrix.fromVersion fails at runtime for tests using that version. " +
                    "Missing: $missing. Available matrix versions: $matrixVersions"
        )
    }
}

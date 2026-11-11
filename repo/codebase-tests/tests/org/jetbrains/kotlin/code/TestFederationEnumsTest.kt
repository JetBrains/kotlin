/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.jetbrains.kotlin.test.KtAssert.fail
import org.jetbrains.kotlin.testFederation.Domain
import org.jetbrains.kotlin.testFederation.NightlyTest
import org.jetbrains.kotlin.testFederation.TestSubset
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import kotlin.test.Test

/**
 * Verifies that the TeamCity configuration declares the same [Domain] and [TestSubset] enums as kotlin.git.
 *
 * The TeamCity DSL keeps its own copies of both enums. Their entries are passed in as
 * `test.federation.teamcity.domains` and `test.federation.teamcity.test.subsets`
 */
@NightlyTest
@EnabledIfSystemProperty(named = "teamcity", matches = "true")
class TestFederationEnumsTest {

    @EnabledIfSystemProperty(named = "test.federation.teamcity.domains", matches = ".+") // Remove after a 'phase-in' period of this test
    @Test
    fun `Domain enum is in sync with TeamCity`() {
        assertInSync(
            enumName = "Domain",
            expected = Domain.entries.map { it.name },
            actual = teamCityEntries("test.federation.teamcity.domains"),
        )
    }

    @EnabledIfSystemProperty(named = "test.federation.teamcity.test.subsets", matches = ".+") // Remove after a 'phase-in' period of this test
    @Test
    fun `TestSubset enum is in sync with TeamCity`() {
        assertInSync(
            enumName = "TestSubset",
            expected = TestSubset.entries.map { it.name },
            actual = teamCityEntries("test.federation.teamcity.test.subsets"),
        )
    }

    private fun teamCityEntries(property: String): List<String> =
        (System.getProperty(property) ?: error("Missing '$property' property")).split(";")

    private fun assertInSync(enumName: String, expected: List<String>, actual: List<String>) {
        if (expected == actual) return

        val missingInTeamCity = expected - actual.toSet()
        val unknownInKotlinGit = actual - expected.toSet()

        fail(buildString {
            appendLine("'$enumName' in kotlin.git and in the current TeamCity configuration are not in sync.")
            if (missingInTeamCity.isNotEmpty()) {
                appendLine("  Missing in TeamCity: ${missingInTeamCity.joinToString(", ")}")
            }
            if (unknownInKotlinGit.isNotEmpty()) {
                appendLine("  Unknown in kotlin.git: ${unknownInKotlinGit.joinToString(", ")}")
            }
            if (missingInTeamCity.isEmpty() && unknownInKotlinGit.isEmpty()) {
                appendLine("  The entries are the same, but their order differs.")
            }
            appendLine("  expected (kotlin.git): ${expected.joinToString(", ")}")
            appendLine("  actual (TeamCity):     ${actual.joinToString(", ")}")
            appendLine("Please update the '$enumName' enum in the TeamCity configuration ")
            appendLine("so that it declares the same entries in the same order as kotlin.git.")
        })
    }
}

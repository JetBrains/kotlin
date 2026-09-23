/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Provides tests with different selection annotations for the Test Federation functional tests.
 * The functional tests run this class and check which tests ran.
 */
class PseudoTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            println("PseudoTest.beforeAll executed")
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            println("PseudoTest.afterAll executed")
        }
    }

    @Test
    fun `domain test`() {
        if (autoSmokeTestPercentage == 0) {
            val subsets = testFederationSubsets
            assertTrue(
                TestSubset.AllTests in subsets,
                "Expected 'AllTests' in requested subsets, but was: $subsets"
            )
        }
    }

    @MustRunAlways
    @Test
    fun `smoke test`() {

    }

    @MustRunOnChangesInJs
    @Test
    fun `js contract test`() {
        val subsets = testFederationSubsets
        if (TestSubset.ContractTestsForJs !in subsets && autoSmokeTestPercentage == 0 && TestSubset.AllTests !in subsets) {
            error("Expected 'ContractTestsForJs' or 'AllTests' in requested subsets, but was: $subsets")
        }
    }

    @MustRunOnChangesInWasm
    @Test
    fun `wasm contract test`() {
        val subsets = testFederationSubsets
        if (TestSubset.ContractTestsForWasm !in subsets && autoSmokeTestPercentage == 0 && TestSubset.AllTests !in subsets) {
            error("Expected 'ContractTestsForWasm' or 'AllTests' in requested subsets, but was: $subsets")
        }
    }

    @MustRunOnChangesInGradle
    @Test
    fun `gradle contract test`() {
        val subsets = testFederationSubsets
        if (TestSubset.ContractTestsForGradle !in subsets && autoSmokeTestPercentage == 0 && TestSubset.AllTests !in subsets) {
            error("Expected 'ContractTestsForGradle' or 'AllTests' in requested subsets, but was: $subsets")
        }
    }

    @NightlyTest
    @Test
    fun `nightly test`() {
        assertTrue(testFederationNightly)
    }
}

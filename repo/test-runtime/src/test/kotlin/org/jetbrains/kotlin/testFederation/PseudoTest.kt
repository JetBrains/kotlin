/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.jetbrains.kotlin.testFederation.TestSubset.*
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
    fun `plain test`() {
        if (autoSmokeTestPercentage == 0) {
            val subsets = testFederationSubsets
            assertTrue(
                PlainTests in subsets,
                "Expected 'PlainTests' in requested subsets, but was: $subsets"
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
        if (testFederationSubsets.containsNone(ContractTestsForJs, PlainTests, AllTests) && autoSmokeTestPercentage == 0) {
            error("Expected ContractTestsForJs or PlainTests or AllTests in requested subsets, but was: $testFederationSubsets")
        }
    }

    @MustRunOnChangesInWasm
    @Test
    fun `wasm contract test`() {
        if (testFederationSubsets.containsNone(ContractTestsForWasm, PlainTests, AllTests) && autoSmokeTestPercentage == 0) {
            error("Expected ContractTestsForWasm or PlainTests or AllTests in requested subsets, but was: $testFederationSubsets")
        }
    }

    @MustRunOnChangesInGradle
    @Test
    fun `gradle contract test`() {
        if (testFederationSubsets.containsNone(ContractTestsForGradle, PlainTests, AllTests) && autoSmokeTestPercentage == 0) {
            error("Expected ContractTestsForGradle or PlainTests or AllTests in requested subsets, but was: $testFederationSubsets")
        }
    }

    @NightlyTest
    @Test
    fun `nightly test`() {
        assertTrue(testFederationNightly)
    }

    @MustRunAlways
    @MustRunOnChangesInJs
    @Test
    fun `smoke js contract test`() {
        if (testFederationSubsets.containsNone(SmokeTests, ContractTestsForJs, AllTests) && autoSmokeTestPercentage == 0) {
            error("Expected SmokeTests or ContractTestsForJs or AllTests in requested subsets, but was: $testFederationSubsets")
        }
    }

    @Suppress("JUnitMixedFramework")
    @org.junit.Test
    fun `junit4 test`() {
        println("Executed: junit4 test")
    }
}

private fun Set<TestSubset>.containsNone(vararg subset: TestSubset): Boolean =
    subset.toSet().intersect(this).isEmpty()

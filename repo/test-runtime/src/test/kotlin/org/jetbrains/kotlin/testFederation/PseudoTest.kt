/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Provides tests with different selection annotations for the Test Federation functional tests.
 * The functional tests run this class and check which tests ran.
 */
class PseudoTest {
    @Test
    fun `domain test`() {
        if (autoSmokeTestPercentage == 0) {
            assertEquals(TestFederationMode.Full, testFederationMode)
        }
    }

    @MustRunAlways
    @Test
    fun `smoke test`() {

    }

    @MustRunOnChangesInJs
    @Test
    fun `js contract test`() {
        if (testFederationMode == TestFederationMode.Full) return
        val changed = testFederationChangedDomains ?: error("Missing 'testFederationAffectedDomains'")
        if (Domain.Js !in changed && autoSmokeTestPercentage == 0) error("Expected 'Js' in affected domains, but was: $changed")
    }

    @MustRunOnChangesInWasm
    @Test
    fun `wasm contract test`() {
        if (testFederationMode == TestFederationMode.Full) return
        val changed = testFederationChangedDomains ?: error("Missing 'testFederationAffectedDomains'")
        if (Domain.Wasm !in changed && autoSmokeTestPercentage == 0) error("Expected 'Wasm' in affected domains, but was: $changed")
    }

    @MustRunOnChangesInGradle
    @Test
    fun `gradle contract test`() {
        if (testFederationMode == TestFederationMode.Full) return
        val changed = testFederationChangedDomains ?: error("Missing 'testFederationAffectedDomains'")
        if (Domain.Gradle !in changed && autoSmokeTestPercentage == 0) error("Expected 'Gradle' in affected domains, but was: $changed")
    }

    @NightlyTest
    @Test
    fun `nightly test`() {
        assertTrue(testFederationNightly)
    }
}

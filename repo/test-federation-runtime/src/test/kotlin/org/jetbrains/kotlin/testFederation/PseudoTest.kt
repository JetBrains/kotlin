/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * This test is a 'PseudoTest'.
 * It will be executed by another test to verify if the contract and smoke test system works correctly.
 */
class PseudoTest {
    @Test
    fun `domain test`() {
        assertEquals(TestFederationMode.Full, testFederationMode)
    }

    @SmokeTest
    @Test
    fun `smoke test`() {

    }

    @AffectedByJs
    @Test
    fun `js contract test`() {
        if (testFederationMode == TestFederationMode.Full) return
        val affected = testFederationAffectedDomains ?: error("Missing 'testFederationAffectedDomains'")
        if (Domain.Js !in affected) error("Expected 'Js' in affected domains, but was: $affected")
    }

    @AffectedByWasm
    @Test
    fun `wasm contract test`() {
        if (testFederationMode == TestFederationMode.Full) return
        val affected = testFederationAffectedDomains ?: error("Missing 'testFederationAffectedDomains'")
        if (Domain.Wasm !in affected) error("Expected 'Wasm' in affected domains, but was: $affected")
    }

    @AffectedByGradle
    @Test
    fun `gradle contract test`() {
        if (testFederationMode == TestFederationMode.Full) return
        val affected = testFederationAffectedDomains ?: error("Missing 'testFederationAffectedDomains'")
        if (Domain.Gradle !in affected) error("Expected 'Gradle' in affected domains, but was: $affected")
    }
}

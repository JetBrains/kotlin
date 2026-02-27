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
    fun `system test`() {
        assertEquals(TestFederationMode.Full, testFederationMode)
    }

    @SmokeTest
    @Test
    fun `smoke test`() {

    }

    @CompilerContract
    @Test
    fun `compiler contract test`() {
        if (testFederationMode == TestFederationMode.Full) return
        val affected = testFederationAffectedSubsystems ?: error("Missing 'testFederationAffectedSubsystems'")
        if (Subsystem.Compiler !in affected) error("Expected 'Compiler' in affected subsystems, but was: $affected")
    }

    @WasmContract
    @Test
    fun `wasm contract test`() {
        if (testFederationMode == TestFederationMode.Full) return
        val affected = testFederationAffectedSubsystems ?: error("Missing 'testFederationAffectedSubsystems'")
        if (Subsystem.Wasm !in affected) error("Expected 'Wasm' in affected subsystems, but was: $affected")
    }
}

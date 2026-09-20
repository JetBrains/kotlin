/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testInventory

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Tests that exist to be recorded into `test-executions.json` and replayed to TeamCity from it.
 *
 * Between them they cover every shape a recording can take, each of which the replay has to do
 * something different with: a test whose class is the suite it sits in, a test in a nested class, a
 * parameterized test whose invocations sit in a container narrower than the class, and the three
 * statuses. Their names and their order are asserted verbatim by the functional tests, see this
 * module's ReadMe.
 */
class TestInventoryFixtureTest {

    @Test
    fun passing() = Unit

    @Test
    @Disabled("Recorded as 'Ignored' and replayed as 'testIgnored'")
    fun ignored() = Unit

    /**
     * Fails only when asked to, and the task tolerates it: a recorded failure in a task that
     * succeeded, which is what a retried flaky test leaves behind on CI.
     */
    @Test
    fun failing() {
        if (System.getProperty("testInventoryFixture.failing").toBoolean()) fail("Failing on purpose")
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2])
    fun parameterized(value: Int) = Unit

    @Nested
    inner class NestedTests {
        @Test
        fun nested() = Unit
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The rules for turning what Gradle reports about a test into what TeamCity calls it.
 *
 * Ports of TeamCity's own - its Gradle runner's init script - so what they have to be right about is
 * cases rather than builds.
 */
class TestExecutionResultUtilsTest {

    @Test
    fun `a name is the class and the method, and the method alone where there is no class`() {
        assertEquals("com.example.SomeTest.passes", "com.example.SomeTest".qualifying("passes"))
        assertEquals("passes", null.qualifying("passes"))
        // How Gradle spells "no class" for a test outside any container; joining leaves a leading dot.
        assertEquals("passes", "".qualifying("passes"))
    }

    @Test
    fun `the method part is the display name where the display name stands for the method`() {
        assertEquals("passes()", teamCityRunnerMethodName(name = "passes()", displayName = "passes()"))

        // A '@DisplayName' says something else entirely, so the method it belongs to is kept too.
        assertEquals(
            "passes(does the thing)",
            teamCityRunnerMethodName(name = "passes()", displayName = "does the thing"),
        )

        // A parameterized invocation: the name is the method, the display name the arguments.
        assertEquals(
            "parameterized([1] 1)",
            teamCityRunnerMethodName(name = "parameterized(int)", displayName = "[1] 1"),
        )

        // A display name that only adds a parameter list says nothing the name does not.
        assertEquals("passes", teamCityRunnerMethodName(name = "passes", displayName = "passes()"))
    }

    @Test
    fun `the suites Gradle inserts itself stand for nothing in a name`() {
        assertTrue(isGradleInsertedSuiteName("Gradle Test Run :some:project:test", taskName = "test"))
        assertTrue(isGradleInsertedSuiteName("Gradle Test Executor 7", taskName = "test"))
        assertTrue(isGradleInsertedSuiteName("Partition 3", taskName = "test"))
        assertTrue(isGradleInsertedSuiteName("test", taskName = "test"))
        assertFalse(isGradleInsertedSuiteName("com.example.SomeTest", taskName = "test"))
    }

    @Test
    fun `a class suite named after the task carries nothing of its own either`() {
        // Kotlin/JS and Kotlin/Wasm report a class suite prefixed with the task's name.
        assertTrue(isSyntheticSuiteName("jsNodeTest.com.example.SomeTest", "jsNodeTest", "com.example.SomeTest"))

        // A plain class suite is not synthetic: dropping it is a decision per test, not per suite.
        assertFalse(isSyntheticSuiteName("com.example.SomeTest", "test", "com.example.SomeTest"))
    }
}

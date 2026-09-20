/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.tasks.testing.TestDescriptor
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The rules for turning what Gradle reports about a test into what TeamCity calls it.
 *
 * Ports of TeamCity's own - its Gradle runner's init script, and the normalization its server
 * applies afterwards - so what they have to be right about is cases rather than builds.
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
        assertEquals("passes()", descriptor(name = "passes()", displayName = "passes()").teamCityRunnerMethodName())

        // A '@DisplayName' says something else entirely, so the method it belongs to is kept too.
        assertEquals(
            "passes(does the thing)",
            descriptor(name = "passes()", displayName = "does the thing").teamCityRunnerMethodName(),
        )

        // A parameterized invocation: the descriptor is the method, the display name the arguments.
        assertEquals(
            "parameterized([1] 1)",
            descriptor(name = "parameterized(int)", displayName = "[1] 1").teamCityRunnerMethodName(),
        )

        // A display name that only adds a parameter list says nothing the name does not.
        assertEquals("passes", descriptor(name = "passes", displayName = "passes()").teamCityRunnerMethodName())
    }

    @Test
    fun `the name TeamCity registers has an empty parameter list dropped, and nothing else`() {
        assertEquals(
            "com.example.SomeTest.passes",
            descriptor("passes()", className = "com.example.SomeTest").toTeamCityRegisteredTestName(),
        )

        assertEquals(
            "com.example.SomeTest.parameterized([1] 1)",
            descriptor("parameterized(int)", className = "com.example.SomeTest", displayName = "[1] 1")
                .toTeamCityRegisteredTestName(),
        )
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

    @Test
    fun `a class suite named after the task is recorded under the bare class`() {
        assertEquals(
            "com.example.SomeTest",
            descriptor("jsNodeTest.com.example.SomeTest", className = "com.example.SomeTest")
                .recordedSuiteName(taskName = "jsNodeTest"),
        )

        // Only that exact spelling is rewritten: a container has a class too, and keeps its own name.
        assertEquals(
            "parameterized(int)",
            descriptor("parameterized(int)", className = "com.example.SomeTest").recordedSuiteName(taskName = "test"),
        )
    }

    @Test
    fun `the recorded suites of a test are the ones Gradle did not insert, its class among them`() {
        val test = descriptor("passes()", className = "com.example.SomeTest", parent = classSuite())

        assertEquals(listOf("com.example.SomeTest"), test.enclosingSuiteNames(taskName = "test"))
    }

    @Test
    fun `the path of a test drops the suite standing for its own class`() {
        val test = descriptor("passes()", className = "com.example.SomeTest", parent = classSuite())

        val path = test.toTestPath(taskName = "test")

        assertEquals(emptyList(), path.suites)
        assertEquals("com.example.SomeTest.passes", path.joinToTeamCityName())
    }

    @Test
    fun `a container narrower than the class keeps both itself and the class in the path`() {
        val container = descriptor("parameterized(int)", className = "com.example.SomeTest", parent = classSuite())
        val invocation = descriptor(
            name = "parameterized(int)",
            className = "com.example.SomeTest",
            displayName = "[1] 1",
            parent = container,
        )

        val path = invocation.toTestPath(taskName = "test")

        assertEquals(listOf("com.example.SomeTest", "parameterized(int)"), path.suites)
        assertEquals(
            "com.example.SomeTest: parameterized(int): com.example.SomeTest.parameterized([1] 1)",
            path.joinToTeamCityName(),
        )
    }

    /** TeamCity reads the apostrophe as the end of the attribute, so such a name is refused outright. */
    @Test
    fun `a name ending in an apostrophe is refused`() {
        val test = descriptor("what a name'")

        val refusal = assertFailsWith<IllegalStateException> { test.toTestPath(taskName = "test") }

        assertContains(refusal.message.orEmpty(), "TW-101796")
    }
}

/** The suites Gradle puts above every test, and the class suite below them. */
private fun classSuite(className: String = "com.example.SomeTest"): TestDescriptor {
    val run = descriptor("Gradle Test Run :some:project:test", composite = true)
    val executor = descriptor("Gradle Test Executor 7", parent = run, composite = true)
    return descriptor(className, className = className, parent = executor, composite = true)
}

private fun descriptor(
    name: String,
    className: String? = null,
    displayName: String = name,
    parent: TestDescriptor? = null,
    composite: Boolean = false,
): TestDescriptor = FakeTestDescriptor(name, className, displayName, parent, composite)

/** What Gradle hands a [org.gradle.api.tasks.testing.TestListener], with nothing behind it. */
private class FakeTestDescriptor(
    private val name: String,
    private val className: String?,
    private val displayName: String,
    private val parent: TestDescriptor?,
    private val composite: Boolean,
) : TestDescriptor {
    override fun getName(): String = name
    override fun getClassName(): String? = className
    override fun getDisplayName(): String = displayName
    override fun getParent(): TestDescriptor? = parent
    override fun isComposite(): Boolean = composite
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What a recording is turned into, asserted on the messages themselves.
 *
 * [TestBuildCacheTeamCityCompatibilityFunctionalTest] asserts the same thing end to end for a real
 * task of a real build; these take the shapes a recording can have - the three states of
 * `className`, a class that is also a container, a status nobody knows, a file that is not there -
 * which a build would have to be bent into producing.
 *
 * The messages are bodies, without the marker and brackets a service message is wrapped in, see
 * [TestExecutionsReplay]. A whole line would be read by the build running these tests as one of its
 * own tests starting, and a failing assertion prints what it compared.
 */
class TestExecutionsReplayTest {

    @TempDir
    lateinit var directory: File

    private val warnings = mutableListOf<String>()

    /**
     * The common case: a test class is a suite in the recording, because that is where Gradle's
     * timing for it hangs, and is part of the test's name to TeamCity rather than a suite of its own.
     */
    @Test
    fun `a test of the class its suite stands for is named after it, and the suite is gone`() {
        val messages = replayOf(
            """
            {
              "suites": [
                {
                  "name": "com.example.SomeTest",
                  "tests": [ { "name": "passes()", "status": "OK", "duration": 7 } ]
                }
              ]
            }
            """
        )

        assertEquals(
            listOf(
                "testStarted name='com.example.SomeTest.passes()' $FLOW",
                "testFinished name='com.example.SomeTest.passes()' duration='7' $FLOW",
            ),
            messages,
        )
        assertEquals(emptyList(), warnings)
    }

    /** An ignored test is one TeamCity is told about between its start and its finish. */
    @Test
    fun `an ignored test is replayed as ignored`() {
        val messages = replayOf(
            """
            {
              "suites": [
                {
                  "name": "com.example.SomeTest",
                  "tests": [ { "name": "skipped()", "status": "Ignored", "duration": 0 } ]
                }
              ]
            }
            """
        )

        assertEquals(
            listOf(
                "testStarted name='com.example.SomeTest.skipped()' $FLOW",
                "testIgnored name='com.example.SomeTest.skipped()' $FLOW",
                "testFinished name='com.example.SomeTest.skipped()' duration='0' $FLOW",
            ),
            messages,
        )
    }

    /**
     * A recorded failure carries a status and nothing else, so the replayed one says where the
     * verdict came from rather than pretending to explain it.
     */
    @Test
    fun `a failed test is replayed as a failure that says where the verdict came from`() {
        val messages = replayOf(
            """
            {
              "suites": [
                {
                  "name": "com.example.SomeTest",
                  "tests": [ { "name": "fails()", "status": "Failure", "duration": 3 } ]
                }
              ]
            }
            """
        )

        assertEquals(
            listOf(
                "testStarted name='com.example.SomeTest.fails()' $FLOW",
                "testFailed name='com.example.SomeTest.fails()' message='Test failed when this task last ran; " +
                        "replayed from cached test results, no failure details recorded' $FLOW",
                "testFinished name='com.example.SomeTest.fails()' duration='3' $FLOW",
            ),
            messages,
        )
    }

    /**
     * A test whose suite is narrower than its class - a parameterized invocation, whose container is
     * the method - names its class itself, and that suite is nothing to do with the class, so it stays.
     */
    @Test
    fun `a test that names its own class keeps the suite it sits in`() {
        val messages = replayOf(
            """
            {
              "suites": [
                {
                  "name": "com.example.SomeTest",
                  "suites": [
                    {
                      "name": "parameterized(int)",
                      "tests": [
                        { "name": "parameterized([1] 1)", "className": "com.example.SomeTest", "status": "OK", "duration": 1 }
                      ]
                    }
                  ]
                }
              ]
            }
            """
        )

        assertEquals(
            listOf(
                "testSuiteStarted name='com.example.SomeTest' $FLOW",
                "testSuiteStarted name='parameterized(int)' $FLOW",
                "testStarted name='com.example.SomeTest.parameterized(|[1|] 1)' $FLOW",
                "testFinished name='com.example.SomeTest.parameterized(|[1|] 1)' duration='1' $FLOW",
                "testSuiteFinished name='parameterized(int)' $FLOW",
                "testSuiteFinished name='com.example.SomeTest' $FLOW",
            ),
            messages,
        )
    }

    /**
     * The third state of `className`: written as an explicit `null` for a test that has no class at
     * all, which is not the same as the absent member meaning "the suite I am in".
     */
    @Test
    fun `a test recorded with no class at all keeps the bare name`() {
        val messages = replayOf(
            """
            {
              "suites": [
                {
                  "name": "a suite of no class",
                  "tests": [ { "name": "classless", "className": null, "status": "OK", "duration": 2 } ]
                }
              ]
            }
            """
        )

        assertEquals(
            listOf(
                "testSuiteStarted name='a suite of no class' $FLOW",
                "testStarted name='classless' $FLOW",
                "testFinished name='classless' duration='2' $FLOW",
                "testSuiteFinished name='a suite of no class' $FLOW",
            ),
            messages,
        )
    }

    /**
     * The rule that makes collapsing a class per test rather than per suite worth the trouble: a
     * class that also holds a nested class disappears for its own tests and stays a suite for the
     * nested one's.
     */
    @Test
    fun `a class stays a suite for the tests of the class nested in it`() {
        val messages = replayOf(
            """
            {
              "suites": [
                {
                  "name": "com.example.SomeTest",
                  "suites": [
                    {
                      "name": "$NESTED_CLASS",
                      "tests": [ { "name": "nested()", "status": "OK", "duration": 4 } ]
                    }
                  ],
                  "tests": [ { "name": "outer()", "status": "OK", "duration": 5 } ]
                }
              ]
            }
            """
        )

        assertEquals(
            listOf(
                "testStarted name='com.example.SomeTest.outer()' $FLOW",
                "testFinished name='com.example.SomeTest.outer()' duration='5' $FLOW",
                "testSuiteStarted name='com.example.SomeTest' $FLOW",
                "testStarted name='$NESTED_CLASS.nested()' $FLOW",
                "testFinished name='$NESTED_CLASS.nested()' duration='4' $FLOW",
                "testSuiteFinished name='com.example.SomeTest' $FLOW",
            ),
            messages,
        )
    }

    /**
     * Why the recorded order is load bearing: a retried test is recorded once per attempt, and
     * replaying the attempts as they happened is what lets TeamCity apply its own retry handling.
     */
    @Test
    fun `the attempts of a retried test are replayed in the order they were recorded`() {
        val messages = replayOf(
            """
            {
              "suites": [
                {
                  "name": "com.example.FlakyTest",
                  "tests": [
                    { "name": "flaky()", "status": "Failure", "duration": 1 },
                    { "name": "flaky()", "status": "OK", "duration": 2 }
                  ]
                }
              ]
            }
            """
        )

        assertEquals(
            listOf("testStarted", "testFailed", "testFinished", "testStarted", "testFinished"),
            messages.map { it.substringBefore(' ') },
        )
    }

    /** Every character a service message attribute cannot carry as it is, in one name. */
    @Test
    fun `a name is escaped the way a service message attribute has to be`() {
        val messages = replayOf(
            """
            {
              "suites": [
                {
                  "name": "com.example.SomeTest",
                  "tests": [ { "name": "awkward(|'[a]'\n)", "status": "OK", "duration": 0 } ]
                }
              ]
            }
            """
        )

        assertEquals(
            "testStarted name='com.example.SomeTest.awkward(|||'|[a|]|'|n)' $FLOW",
            messages.first(),
        )
    }

    /** One service replays every task of a build, so a replay has to say which task it is about. */
    @Test
    fun `the flow of a replay is the task's own`() {
        val recording = write(
            """
            {
              "suites": [
                {
                  "name": "com.example.SomeTest",
                  "tests": [ { "name": "passes()", "status": "OK", "duration": 0 } ]
                }
              ]
            }
            """
        )

        val messages = TestExecutionsReplay(":another:project:test") { message, _ -> warnings += message }
            .messagesFor(recording)

        assertTrue(
            messages.all { "flowId='TestReplay:another:project:test'" in it },
            "Every message should carry the flow of the task it was recorded for: $messages",
        )
    }

    /**
     * A build that can no longer say what a cached task tested says so and carries on: the tests it
     * cannot report are worth a warning, never a failed build.
     */
    @Test
    fun `a recording that is not there is replayed as nothing`() {
        val missing = File(directory, "not-written.json")

        val messages = TestExecutionsReplay(TASK_PATH) { message, _ -> warnings += message }.messagesFor(missing)

        assertEquals(emptyList(), messages)
        assertContains(warnings.single(), "does not exist")
    }

    /** The same for a recording that is there and cannot be made sense of. */
    @Test
    fun `a recording that cannot be read is replayed as nothing`() {
        val messages = replayOf("This is not the JSON you are looking for")

        assertEquals(emptyList(), messages)
        assertContains(warnings.single(), "Cannot replay the tests of $TASK_PATH")
    }

    /**
     * A status this does not know is a recording from a newer format, or a bug. The test still
     * happened, so it is still reported; what it came to is what cannot be said.
     */
    @Test
    fun `a status nobody knows is replayed as neither passed nor failed`() {
        val messages = replayOf(
            """
            {
              "suites": [
                {
                  "name": "com.example.SomeTest",
                  "tests": [ { "name": "puzzling()", "status": "Inconclusive", "duration": 0 } ]
                }
              ]
            }
            """
        )

        assertEquals(
            listOf(
                "testStarted name='com.example.SomeTest.puzzling()' $FLOW",
                "testFinished name='com.example.SomeTest.puzzling()' duration='0' $FLOW",
            ),
            messages,
        )
        assertContains(warnings.single(), "Unknown status 'Inconclusive' of test com.example.SomeTest.puzzling()")
    }

    private fun replayOf(document: String): List<String> =
        TestExecutionsReplay(TASK_PATH) { message, _ -> warnings += message }.messagesFor(write(document))

    private fun write(document: String): File =
        File(directory, "test-executions.json").apply { writeText(document.trimIndent()) }
}

private const val TASK_PATH = ":some:project:test"

/** A nested test class, spelled the way the JVM names one. */
private const val NESTED_CLASS = $$"com.example.SomeTest$Nested"

/** How every message of a replay of [TASK_PATH] ends. */
private const val FLOW = "flowId='TestReplay$TASK_PATH'"

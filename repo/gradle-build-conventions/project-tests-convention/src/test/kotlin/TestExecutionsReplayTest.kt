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
 * What a recording is turned into: the shapes one can have, which a build would have to be bent into
 * producing. [TestBuildCacheTeamCityCompatibilityFunctionalTest] asserts the same end to end.
 *
 * The recordings are written as [TestExecutionsListener] writes them, one record per line, the
 * suite nesting and all - these tests are where that format is read back, so they spell it out.
 *
 * The messages are bodies, without the marker a service message is wrapped in, see
 * [TestExecutionsReplay]: a failing assertion prints what it compared.
 */
class TestExecutionsReplayTest {

    @TempDir
    lateinit var directory: File

    private val warnings = mutableListOf<String>()

    /** The common case: a class is a suite in the recording, and part of the test's name to TeamCity. */
    @Test
    fun `a test of the class its suite stands for is named after it, and the suite is gone`() {
        val messages = replayOf(
            """
            {"type":"header","formatVersion":1,"taskPath":":some:project:test"}
            {"type":"suite","id":1,"name":"Gradle Test Run :some:project:test"}
            {"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor 1"}
            {"type":"suite","id":3,"parent":2,"name":"$TEST_CLASS","className":"$TEST_CLASS"}
            {"type":"test","parent":3,"name":"passes()","resultType":"SUCCESS","startTime":10,"endTime":17}
            {"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":9,"endTime":18}
            {"type":"suiteEnd","id":2,"resultType":"SUCCESS","startTime":8,"endTime":19}
            {"type":"suiteEnd","id":1,"resultType":"SUCCESS","startTime":7,"endTime":20}
            """
        )

        assertEquals(
            listOf(
                "testStarted name='$TEST_CLASS.passes()' $FLOW",
                "testFinished name='$TEST_CLASS.passes()' duration='7' $FLOW",
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
            {"type":"header","formatVersion":1,"taskPath":":some:project:test"}
            {"type":"suite","id":1,"name":"Gradle Test Run :some:project:test"}
            {"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor 1"}
            {"type":"suite","id":3,"parent":2,"name":"$TEST_CLASS","className":"$TEST_CLASS"}
            {"type":"test","parent":3,"name":"skipped()","resultType":"SKIPPED","startTime":0,"endTime":0}
            {"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":0,"endTime":0}
            {"type":"suiteEnd","id":2,"resultType":"SUCCESS","startTime":0,"endTime":0}
            {"type":"suiteEnd","id":1,"resultType":"SUCCESS","startTime":0,"endTime":0}
            """
        )

        assertEquals(
            listOf(
                "testStarted name='$TEST_CLASS.skipped()' $FLOW",
                "testIgnored name='$TEST_CLASS.skipped()' $FLOW",
                "testFinished name='$TEST_CLASS.skipped()' duration='0' $FLOW",
            ),
            messages,
        )
    }

    /** A recording keeps a result and no stack trace, so the replay says where the verdict came from. */
    @Test
    fun `a failed test is replayed as a failure that says where the verdict came from`() {
        val messages = replayOf(
            """
            {"type":"header","formatVersion":1,"taskPath":":some:project:test"}
            {"type":"suite","id":1,"name":"Gradle Test Run :some:project:test"}
            {"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor 1"}
            {"type":"suite","id":3,"parent":2,"name":"$TEST_CLASS","className":"$TEST_CLASS"}
            {"type":"test","parent":3,"name":"fails()","resultType":"FAILURE","startTime":3,"endTime":6}
            {"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":2,"endTime":7}
            {"type":"suiteEnd","id":2,"resultType":"SUCCESS","startTime":1,"endTime":8}
            {"type":"suiteEnd","id":1,"resultType":"SUCCESS","startTime":0,"endTime":9}
            """
        )

        assertEquals(
            listOf(
                "testStarted name='$TEST_CLASS.fails()' $FLOW",
                "testFailed name='$TEST_CLASS.fails()' message='Test failed when this task last ran; " +
                        "replayed from cached test results, no failure details recorded' $FLOW",
                "testFinished name='$TEST_CLASS.fails()' duration='3' $FLOW",
            ),
            messages,
        )
    }

    /**
     * A suite narrower than the class - a parameterized invocation's container - is not the class, so it stays.
     *
     * Its tests carry the class explicitly in a recording by [TestExecutionsListener]; here it is
     * left for the parent suite to say, the other spelling a recording keeps.
     */
    @Test
    fun `a test that names its own class keeps the suite it sits in`() {
        val messages = replayOf(
            """
            {"type":"header","formatVersion":1,"taskPath":":some:project:test"}
            {"type":"suite","id":1,"name":"Gradle Test Run :some:project:test"}
            {"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor 1"}
            {"type":"suite","id":3,"parent":2,"name":"$TEST_CLASS","className":"$TEST_CLASS"}
            {"type":"suite","id":4,"parent":3,"name":"parameterized(int)"}
            {"type":"test","parent":4,"name":"parameterized(int)","displayName":"[1] 1","resultType":"SUCCESS","startTime":0,"endTime":1}
            {"type":"suiteEnd","id":4,"resultType":"SUCCESS","startTime":0,"endTime":1}
            {"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":0,"endTime":1}
            {"type":"suiteEnd","id":2,"resultType":"SUCCESS","startTime":0,"endTime":1}
            {"type":"suiteEnd","id":1,"resultType":"SUCCESS","startTime":0,"endTime":1}
            """
        )

        assertEquals(
            listOf(
                "testSuiteStarted name='$TEST_CLASS' $FLOW",
                "testSuiteStarted name='parameterized(int)' $FLOW",
                "testStarted name='$TEST_CLASS.parameterized(|[1|] 1)' $FLOW",
                "testFinished name='$TEST_CLASS.parameterized(|[1|] 1)' duration='1' $FLOW",
                "testSuiteFinished name='parameterized(int)' $FLOW",
                "testSuiteFinished name='$TEST_CLASS' $FLOW",
            ),
            messages,
        )
    }

    /** The third state of `className`: an explicit `null`, unlike the absent member meaning "the suite I am in". */
    @Test
    fun `a test recorded with no class at all keeps the bare name`() {
        val messages = replayOf(
            """
            {"type":"header","formatVersion":1,"taskPath":":some:project:test"}
            {"type":"suite","id":1,"name":"Gradle Test Run :some:project:test"}
            {"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor 1"}
            {"type":"suite","id":3,"parent":2,"name":"a suite of no class"}
            {"type":"test","parent":3,"name":"classless","className":null,"resultType":"SUCCESS","startTime":0,"endTime":2}
            {"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":0,"endTime":2}
            {"type":"suiteEnd","id":2,"resultType":"SUCCESS","startTime":0,"endTime":2}
            {"type":"suiteEnd","id":1,"resultType":"SUCCESS","startTime":0,"endTime":2}
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

    /** Why a class is collapsed per test and not per suite: it stays a suite for a nested class's tests. */
    @Test
    fun `a class stays a suite for the tests of the class nested in it`() {
        val messages = replayOf(
            """
            {"type":"header","formatVersion":1,"taskPath":":some:project:test"}
            {"type":"suite","id":1,"name":"Gradle Test Run :some:project:test"}
            {"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor 1"}
            {"type":"suite","id":3,"parent":2,"name":"$TEST_CLASS","className":"$TEST_CLASS"}
            {"type":"test","parent":3,"name":"outer()","resultType":"SUCCESS","startTime":0,"endTime":5}
            {"type":"suite","id":4,"parent":3,"name":"$NESTED_CLASS","className":"$NESTED_CLASS"}
            {"type":"test","parent":4,"name":"nested()","resultType":"SUCCESS","startTime":0,"endTime":4}
            {"type":"suiteEnd","id":4,"resultType":"SUCCESS","startTime":0,"endTime":4}
            {"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":0,"endTime":5}
            {"type":"suiteEnd","id":2,"resultType":"SUCCESS","startTime":0,"endTime":5}
            {"type":"suiteEnd","id":1,"resultType":"SUCCESS","startTime":0,"endTime":5}
            """
        )

        assertEquals(
            listOf(
                "testStarted name='$TEST_CLASS.outer()' $FLOW",
                "testFinished name='$TEST_CLASS.outer()' duration='5' $FLOW",
                "testSuiteStarted name='$TEST_CLASS' $FLOW",
                "testStarted name='$NESTED_CLASS.nested()' $FLOW",
                "testFinished name='$NESTED_CLASS.nested()' duration='4' $FLOW",
                "testSuiteFinished name='$TEST_CLASS' $FLOW",
            ),
            messages,
        )
    }

    /**
     * The suites a task names after itself carry nothing either: Kotlin/JS and Kotlin/Wasm report a
     * class suite as `<task name>.<class name>`, which TeamCity never sees.
     */
    @Test
    fun `a suite a task names after itself is gone, and the class is part of the name`() {
        val messages = TestExecutionsReplay(":some:project:jsNodeTest", FLOW_ID) { message, _ -> warnings += message }
            .messagesFor(
                write(
                    """
                    {"type":"header","formatVersion":1,"taskPath":":some:project:jsNodeTest"}
                    {"type":"suite","id":1,"name":"Gradle Test Run :some:project:jsNodeTest"}
                    {"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor 1"}
                    {"type":"suite","id":3,"parent":2,"name":"jsNodeTest.$TEST_CLASS","className":"$TEST_CLASS"}
                    {"type":"test","parent":3,"name":"passes()","resultType":"SUCCESS","startTime":0,"endTime":1}
                    {"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":0,"endTime":1}
                    {"type":"suiteEnd","id":2,"resultType":"SUCCESS","startTime":0,"endTime":1}
                    {"type":"suiteEnd","id":1,"resultType":"SUCCESS","startTime":0,"endTime":1}
                    """
                )
            )
            .toList()

        assertEquals(
            listOf(
                "testStarted name='$TEST_CLASS.passes()' $FLOW",
                "testFinished name='$TEST_CLASS.passes()' duration='1' $FLOW",
            ),
            messages,
        )
    }

    /** A retried test is recorded once per attempt, and the attempts are replayed as they happened. */
    @Test
    fun `the attempts of a retried test are replayed in the order they were recorded`() {
        val messages = replayOf(
            """
            {"type":"header","formatVersion":1,"taskPath":":some:project:test"}
            {"type":"suite","id":1,"name":"Gradle Test Run :some:project:test"}
            {"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor 1"}
            {"type":"suite","id":3,"parent":2,"name":"com.example.FlakyTest","className":"com.example.FlakyTest"}
            {"type":"test","parent":3,"name":"flaky()","resultType":"FAILURE","startTime":0,"endTime":1}
            {"type":"test","parent":3,"name":"flaky()","resultType":"SUCCESS","startTime":0,"endTime":2}
            {"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":0,"endTime":2}
            {"type":"suiteEnd","id":2,"resultType":"SUCCESS","startTime":0,"endTime":2}
            {"type":"suiteEnd","id":1,"resultType":"SUCCESS","startTime":0,"endTime":2}
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
            {"type":"header","formatVersion":1,"taskPath":":some:project:test"}
            {"type":"suite","id":1,"name":"Gradle Test Run :some:project:test"}
            {"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor 1"}
            {"type":"suite","id":3,"parent":2,"name":"$TEST_CLASS","className":"$TEST_CLASS"}
            {"type":"test","parent":3,"name":"awkward(|'[a]'\n)","resultType":"SUCCESS","startTime":0,"endTime":0}
            {"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":0,"endTime":0}
            {"type":"suiteEnd","id":2,"resultType":"SUCCESS","startTime":0,"endTime":0}
            {"type":"suiteEnd","id":1,"resultType":"SUCCESS","startTime":0,"endTime":0}
            """
        )

        assertEquals(
            "testStarted name='$TEST_CLASS.awkward(|||'|[a|]|'|n)' $FLOW",
            messages.first(),
        )
    }

    /** One service replays every task of a build, and tells them apart by the flow it hands each. */
    @Test
    fun `every message carries the flow the replay was given`() {
        val recording = write(
            """
            {"type":"header","formatVersion":1,"taskPath":":another:project:test"}
            {"type":"suite","id":1,"name":"Gradle Test Run :another:project:test"}
            {"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor 1"}
            {"type":"suite","id":3,"parent":2,"name":"$TEST_CLASS","className":"$TEST_CLASS"}
            {"type":"test","parent":3,"name":"passes()","resultType":"SUCCESS","startTime":0,"endTime":0}
            {"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":0,"endTime":0}
            {"type":"suiteEnd","id":2,"resultType":"SUCCESS","startTime":0,"endTime":0}
            {"type":"suiteEnd","id":1,"resultType":"SUCCESS","startTime":0,"endTime":0}
            """
        )

        val messages = TestExecutionsReplay(":another:project:test", "R7") { message, _ -> warnings += message }
            .messagesFor(recording)
            .toList()

        assertTrue(
            messages.all { "flowId='R7'" in it },
            "Every message should carry the flow the replay was given: $messages",
        )
    }

    /** Not being able to say what a cached task tested is worth a warning, never a failed build. */
    @Test
    fun `a recording that is not there is replayed as nothing`() {
        val missing = File(directory, "not-written.jsonl")

        val messages = TestExecutionsReplay(TASK_PATH, FLOW_ID) { message, _ -> warnings += message }
            .messagesFor(missing)
            .toList()

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
     * Recordings cross versions of this code - the build cache is shared by branches - so the
     * format a file is recorded in is written down in it, and a version this cannot read is
     * replayed as nothing rather than guessed at.
     */
    @Test
    fun `a recording of a later format is replayed as nothing`() {
        val failures = mutableListOf<Throwable?>()
        val messages = TestExecutionsReplay(TASK_PATH, FLOW_ID) { message, failure ->
            warnings += message
            failures += failure
        }.messagesFor(
            write(
                """
                {"type":"header","formatVersion":2,"taskPath":":some:project:test"}
                """
            )
        ).toList()

        assertEquals(emptyList(), messages)
        assertContains(warnings.single(), "Cannot replay the tests of $TASK_PATH")
        assertContains(failures.single()!!.message.toString(), "format version 2")
    }

    /** Started and finished with nothing in between is how TeamCity is told a test passed. */
    @Test
    fun `a result nobody knows is left out rather than replayed as a pass`() {
        val messages = replayOf(
            """
            {"type":"header","formatVersion":1,"taskPath":":some:project:test"}
            {"type":"suite","id":1,"name":"Gradle Test Run :some:project:test"}
            {"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor 1"}
            {"type":"suite","id":3,"parent":2,"name":"$TEST_CLASS","className":"$TEST_CLASS"}
            {"type":"test","parent":3,"name":"puzzling()","resultType":"INCONCLUSIVE","startTime":0,"endTime":0}
            {"type":"test","parent":3,"name":"passes()","resultType":"SUCCESS","startTime":0,"endTime":7}
            {"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":0,"endTime":7}
            {"type":"suiteEnd","id":2,"resultType":"SUCCESS","startTime":0,"endTime":7}
            {"type":"suiteEnd","id":1,"resultType":"SUCCESS","startTime":0,"endTime":7}
            """
        )

        // The tests around it are replayed just the same: one unreadable record is not the file.
        assertEquals(
            listOf(
                "testStarted name='$TEST_CLASS.passes()' $FLOW",
                "testFinished name='$TEST_CLASS.passes()' duration='7' $FLOW",
            ),
            messages,
        )
        assertContains(warnings.single(), "Unknown result type 'INCONCLUSIVE' of test $TEST_CLASS.puzzling()")
    }

    /** Collected, so that a test sees the warnings a replay produced as it went. */
    private fun replayOf(document: String): List<String> =
        TestExecutionsReplay(TASK_PATH, FLOW_ID) { message, _ -> warnings += message }
            .messagesFor(write(document))
            .toList()

    private fun write(document: String): File =
        File(directory, "test-executions.jsonl").apply { writeText(document.trimIndent() + "\n") }
}

private const val TASK_PATH = ":some:project:test"

private const val TEST_CLASS = "com.example.SomeTest"

/** A nested test class, spelled the way the JVM names one. */
private const val NESTED_CLASS = $$"com.example.SomeTest$Nested"

/** The flow id the tests hand a replay, the kind the service numbers per build. */
private const val FLOW_ID = "R1"

/** How every message of a replay carrying [FLOW_ID] ends. */
private const val FLOW = "flowId='$FLOW_ID'"

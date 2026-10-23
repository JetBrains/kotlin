/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import groovy.json.JsonSlurper
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult.ResultType.FAILURE
import org.gradle.api.tasks.testing.TestResult.ResultType.SKIPPED
import org.gradle.api.tasks.testing.TestResult.ResultType.SUCCESS
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TASK_NAME = "test"
private const val TASK_PATH = ":some:project:test"
private const val TEST_CLASS = "com.example.SomeTest"
private const val NESTED_CLASS = $$"$$TEST_CLASS$Nested"
private const val OTHER_CLASS = "com.example.OtherTest"

class TestExecutionsListenerTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `every event is recorded as Gradle reports it`() {
        val listener = listener()
        val run = suite("Gradle Test Run $TASK_PATH")
        val executor = suite("Gradle Test Executor 1", parent = run)
        val testClass = descriptor(
            TEST_CLASS, TEST_CLASS, displayName = "SomeTest", parent = executor, composite = true,
            source = classSource(TEST_CLASS),
        )
        val passes = descriptor("passes()", TEST_CLASS, parent = testClass, source = methodSource(TEST_CLASS, "passes"))
        val skipped = descriptor("skipped()", TEST_CLASS, parent = testClass, source = methodSource(TEST_CLASS, "skipped"))

        listener.beforeSuite(run)
        listener.beforeSuite(executor)
        listener.beforeSuite(testClass)
        listener.beforeTest(passes)
        listener.afterTest(passes, result(SUCCESS, startTime = 1010, endTime = 1013))
        listener.beforeTest(skipped)
        listener.afterTest(skipped, result(SKIPPED, startTime = 1014, endTime = 1014))
        listener.afterSuite(testClass, result(SUCCESS, startTime = 1000, endTime = 1020))
        listener.afterSuite(executor, result(SUCCESS, startTime = 990, endTime = 1025))
        listener.afterSuite(run, result(SUCCESS, startTime = 980, endTime = 1030))

        assertEquals(
            listOf(
                """{"type":"header","formatVersion":1,"taskPath":":some:project:test"}""",
                """{"type":"suite","id":1,"name":"Gradle Test Run :some:project:test"}""",
                """{"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor 1"}""",
                """{"type":"suite","id":3,"parent":2,"name":"com.example.SomeTest","displayName":"SomeTest","className":"com.example.SomeTest","source":{"kind":"class"}}""",
                """{"type":"test","parent":3,"name":"passes()","source":{"kind":"method","methodName":"passes"},"resultType":"SUCCESS","startTime":1010,"endTime":1013}""",
                """{"type":"test","parent":3,"name":"skipped()","source":{"kind":"method","methodName":"skipped"},"resultType":"SKIPPED","startTime":1014,"endTime":1014}""",
                """{"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":1000,"endTime":1020}""",
                """{"type":"suiteEnd","id":2,"resultType":"SUCCESS","startTime":990,"endTime":1025}""",
                """{"type":"suiteEnd","id":1,"resultType":"SUCCESS","startTime":980,"endTime":1030}""",
            ),
            recordedLines(),
        )
    }

    @Test
    fun `a class name is written only where it is not the parent's`() {
        val listener = listener()
        val run = suite("Gradle Test Run $TASK_PATH")
        val testClass = suite(TEST_CLASS, parent = run, className = TEST_CLASS)
        // JUnit 5 reports a parameterized method as a suite of its own, carrying the class of its tests.
        val parameterized = suite("checks(String)", parent = testClass, className = TEST_CLASS)
        val invocation = descriptor("[1] a", TEST_CLASS, parent = parameterized)
        val nestedClass = suite(NESTED_CLASS, parent = testClass, className = NESTED_CLASS)
        val nestedTest = descriptor("nested()", NESTED_CLASS, parent = nestedClass)
        val classless = descriptor("classless", className = null, parent = testClass)
        val emptyClass = descriptor("emptyClass", className = "", parent = run)
        val elsewhere = descriptor("elsewhere()", TEST_CLASS, parent = testClass, source = methodSource(OTHER_CLASS, "elsewhere"))

        listener.inSuite(run) {
            listener.inSuite(testClass) {
                listener.inSuite(parameterized) { listener.passed(invocation) }
                listener.inSuite(nestedClass) { listener.passed(nestedTest) }
                listener.passed(classless)
                listener.passed(elsewhere)
            }
            listener.passed(emptyClass)
        }

        val records = recorded()
        fun record(name: String) = records.single { it["name"] == name }

        assertEquals(false, record(run.name).containsKey("className"), "A record without a parent has no class to inherit")
        assertEquals(TEST_CLASS, record(TEST_CLASS)["className"])
        assertEquals(false, record("checks(String)").containsKey("className"))
        assertEquals(false, record("[1] a").containsKey("className"))
        assertEquals(NESTED_CLASS, record(NESTED_CLASS)["className"])
        assertEquals(false, record("nested()").containsKey("className"))
        assertEquals(true, record("classless").containsKey("className"), "No class is written out where the parent has one")
        assertEquals(null, record("classless")["className"])
        assertEquals("", record("emptyClass")["className"])
        assertEquals(mapOf("kind" to "method", "className" to OTHER_CLASS, "methodName" to "elsewhere"), record("elsewhere()")["source"])
    }

    @Test
    fun `every kind of source is written with the fields of its kind`() {
        val listener = listener()
        val run = suite("Gradle Test Run $TASK_PATH")
        val sources = listOf(
            fileSource(File("Some.kt"), position(12, column = 3)),
            fileSource(File("Some.kt"), position(12)),
            fileSource(File("Some.kt"), position = null),
            directorySource(File("data")),
            classpathResourceSource("data/some.txt", position(4)),
            otherSource(),
        )

        listener.inSuite(run) {
            sources.forEachIndexed { index, source -> listener.passed(descriptor("test$index", parent = run, source = source)) }
        }

        assertEquals(
            listOf(
                mapOf("kind" to "file", "file" to "Some.kt", "line" to 12, "column" to 3),
                mapOf("kind" to "file", "file" to "Some.kt", "line" to 12),
                mapOf("kind" to "file", "file" to "Some.kt"),
                mapOf("kind" to "directory", "file" to "data"),
                mapOf("kind" to "classpathResource", "classpathResourceName" to "data/some.txt", "line" to 4),
                mapOf("kind" to "other"),
            ),
            recorded().filter { it["type"] == "test" }.map { it["source"] },
        )
    }

    @Test
    fun `the events of forks running in parallel each refer to their own suite`() {
        val listener = listener()
        val run = suite("Gradle Test Run $TASK_PATH")
        val firstExecutor = suite("Gradle Test Executor 1", parent = run)
        val secondExecutor = suite("Gradle Test Executor 2", parent = run)
        val firstClass = suite(TEST_CLASS, parent = firstExecutor, className = TEST_CLASS)
        val secondClass = suite(OTHER_CLASS, parent = secondExecutor, className = OTHER_CLASS)

        listener.beforeSuite(run)
        listener.beforeSuite(firstExecutor)
        listener.beforeSuite(secondExecutor)
        listener.beforeSuite(firstClass)
        listener.beforeSuite(secondClass)
        listener.passed(descriptor("first()", TEST_CLASS, parent = firstClass))
        listener.passed(descriptor("second()", OTHER_CLASS, parent = secondClass))
        listener.afterSuite(secondClass, result(SUCCESS, startTime = 0, endTime = 0))
        listener.passed(descriptor("third()", TEST_CLASS, parent = firstClass))
        listener.afterSuite(firstClass, result(SUCCESS, startTime = 0, endTime = 0))
        listener.afterSuite(secondExecutor, result(SUCCESS, startTime = 0, endTime = 0))
        listener.afterSuite(firstExecutor, result(SUCCESS, startTime = 0, endTime = 0))
        listener.afterSuite(run, result(SUCCESS, startTime = 0, endTime = 0))

        val records = recorded()
        val suiteIds = records.filter { it["type"] == "suite" }.associate { it["name"] to it["id"] }
        val testParents = records.filter { it["type"] == "test" }.map { it["name"] to it["parent"] }
        assertEquals(
            listOf("first()" to suiteIds[TEST_CLASS], "second()" to suiteIds[OTHER_CLASS], "third()" to suiteIds[TEST_CLASS]),
            testParents,
        )
    }

    @Test
    fun `a suite rerun for a retry is a suite of its own, and every attempt is recorded in order`() {
        val listener = listener()
        val run = suite("Gradle Test Run $TASK_PATH")

        listener.inSuite(run) {
            val firstExecutor = suite("Gradle Test Executor 1", parent = run)
            val firstRound = suite(TEST_CLASS, parent = firstExecutor, className = TEST_CLASS)
            listener.inSuite(firstExecutor) {
                listener.inSuite(firstRound) {
                    listener.afterTest(descriptor("flaky()", TEST_CLASS, parent = firstRound), result(FAILURE, 0, 15))
                    listener.passed(descriptor("passes()", TEST_CLASS, parent = firstRound))
                }
            }

            val retryExecutor = suite("Gradle Test Executor 2", parent = run)
            val retryRound = suite(TEST_CLASS, parent = retryExecutor, className = TEST_CLASS)
            listener.inSuite(retryExecutor) {
                listener.inSuite(retryRound) { listener.passed(descriptor("flaky()", TEST_CLASS, parent = retryRound)) }
            }
        }

        val records = recorded()
        val classSuites = records.filter { it["type"] == "suite" && it["name"] == TEST_CLASS }.map { it["id"] }
        assertEquals(2, classSuites.distinct().size, "Each round of a retried suite has an id of its own")
        assertEquals(
            listOf(
                Triple("flaky()", classSuites[0], "FAILURE"),
                Triple("passes()", classSuites[0], "SUCCESS"),
                Triple("flaky()", classSuites[1], "SUCCESS"),
            ),
            records.filter { it["type"] == "test" }.map { Triple(it["name"], it["parent"], it["resultType"]) },
        )
    }

    @Test
    fun `nothing of an earlier run is left in the file`() {
        executionsFile.parentFile.mkdirs()
        executionsFile.writeText("""{"type":"test","name":"fromEarlierRun()"}""" + "\n")

        val listener = listener()
        val run = suite("Gradle Test Run $TASK_PATH")
        listener.inSuite(run) { listener.passed(descriptor("passes()", TEST_CLASS, parent = run)) }

        assertEquals(listOf("header", "suite", "test", "suiteEnd"), recorded().map { it["type"] })
    }

    @Test
    fun `any name survives the file`() {
        val names = listOf(
            "quote \" backslash \\ apostrophe ' colon : hash #",
            "new\nline\r\ntab\tcontrol\u0001delete\u007f",
            "pair 😀, lone high \uD83D, lone low \uDE00, reversed \uDE00\uD83D",
            "non-ASCII: ü ß 中文",
        )

        val listener = listener()
        val run = suite("Gradle Test Run $TASK_PATH")
        listener.inSuite(run) { names.forEach { listener.passed(descriptor(it, TEST_CLASS, parent = run)) } }

        assertEquals(names, recorded().filter { it["type"] == "test" }.map { it["name"] })
        val text = executionsFile.readText(Charsets.UTF_8)
        assertEquals("", text.filterNot { it in ' '..'~' || it == '\n' }, "Everything else is escaped")
        assertEquals(recorded().size, text.lines().count { it.isNotEmpty() }, "Every record is a single line")
    }

    private val executionsFile: File get() = projectDir.resolve("build/test-inventory/$TASK_NAME/test-executions.jsonl")

    private fun listener(): TestExecutionsListener {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        return TestExecutionsListener(TASK_NAME, TASK_PATH, project.provider { projectDir.resolve("build") })
    }

    private fun recordedLines(): List<String> = executionsFile.readLines(Charsets.UTF_8)

    private fun recorded(): List<Map<*, *>> = recordedLines().map { JsonSlurper().parseText(it) as Map<*, *> }
}

private fun suite(name: String, parent: TestDescriptor? = null, className: String? = null): TestDescriptor =
    descriptor(name, className = className, parent = parent, composite = true)

private fun TestExecutionsListener.inSuite(suite: TestDescriptor, events: () -> Unit) {
    beforeSuite(suite)
    events()
    afterSuite(suite, result(SUCCESS, startTime = 0, endTime = 0))
}

private fun TestExecutionsListener.passed(test: TestDescriptor) {
    beforeTest(test)
    afterTest(test, result(SUCCESS, startTime = 0, endTime = 0))
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import groovy.json.JsonSlurper
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult.ResultType.FAILURE
import org.gradle.api.tasks.testing.TestResult.ResultType.SUCCESS
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TASK_NAME = "test"
private const val TASK_PATH = ":some:project:test"
private const val TEST_CLASS = "com.example.SomeTest"

/** What a run leaves in `test-executions.json`, driven through the events Gradle would send. */
class TestExecutionsListenerTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `a suite is recorded with the timing Gradle reported for it`() {
        val listener = listener()
        val run = suite("Gradle Test Run $TASK_PATH")
        val executor = suite("Gradle Test Executor 1", parent = run)
        val testClass = suite(TEST_CLASS, parent = executor)

        listener.afterTest(test("passes()", testClass), result(SUCCESS, durationMillis = 3003))
        listener.afterSuite(testClass, result(SUCCESS, durationMillis = 4116))
        listener.afterSuite(executor, result(SUCCESS, durationMillis = 4302))
        listener.afterSuite(run, result(SUCCESS, durationMillis = 4302))

        val document = recorded()
        assertEquals(4302, document.duration)
        assertEquals(4116, document.suites.single().duration)
    }

    @Test
    fun `a suite rerun for a retry is recorded with what its rounds cost together`() {
        val listener = listener()
        val run = suite("Gradle Test Run $TASK_PATH")

        // The first round runs the whole class, the flaky test failing among the rest of it.
        val firstExecutor = suite("Gradle Test Executor 1", parent = run)
        val firstRound = suite(TEST_CLASS, parent = firstExecutor)
        listener.afterTest(test("flaky()", firstRound), result(FAILURE, durationMillis = 15))
        listener.afterTest(test("passes()", firstRound), result(SUCCESS, durationMillis = 3003))
        listener.afterSuite(firstRound, result(FAILURE, durationMillis = 4116))
        listener.afterSuite(firstExecutor, result(FAILURE, durationMillis = 4302))

        // The retry runs the flaky test alone, under an executor of its own - the only suite telling
        // the rounds apart, and one Gradle inserted, so both rounds are the same class to a reader.
        val retryExecutor = suite("Gradle Test Executor 2", parent = run)
        val retryRound = suite(TEST_CLASS, parent = retryExecutor)
        listener.afterTest(test("flaky()", retryRound), result(SUCCESS, durationMillis = 14))
        listener.afterSuite(retryRound, result(SUCCESS, durationMillis = 42))
        listener.afterSuite(retryExecutor, result(SUCCESS, durationMillis = 206))

        listener.afterSuite(run, result(SUCCESS, durationMillis = 4508))

        val testClass = recorded().suites.single()
        assertEquals(4116L + 42L, testClass.duration, "A retried suite cost the task both of its rounds")
        assertEquals(listOf("flaky()", "passes()", "flaky()"), testClass.testNames)
    }

    /** The listener under test; it resolves its build directory only when it writes. */
    private fun listener(): TestExecutionsListener {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        return TestExecutionsListener(TASK_NAME, TASK_PATH, project.provider { projectDir.resolve("build") })
    }

    private fun recorded(): Map<*, *> =
        JsonSlurper().parse(
            projectDir.resolve("build/test-inventory/$TASK_NAME/test-executions.json"),
            Charsets.UTF_8.name(),
        ) as Map<*, *>
}

/** A suite as Gradle reports one: the class carries its class, the suites Gradle inserts do not. */
private fun suite(name: String, parent: TestDescriptor? = null): TestDescriptor =
    descriptor(name, className = name.takeIf { it == TEST_CLASS }, parent = parent, composite = true)

private fun test(name: String, parent: TestDescriptor): TestDescriptor =
    descriptor(name, className = TEST_CLASS, parent = parent)

private val Map<*, *>.duration: Long get() = (this["duration"] as Number).toLong()
private val Map<*, *>.suites: List<Map<*, *>> get() = (this["suites"] as List<*>).map { it as Map<*, *> }
private val Map<*, *>.testNames: List<String> get() = (this["tests"] as List<*>).map { (it as Map<*, *>)["name"] as String }

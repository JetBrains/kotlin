/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilerResult
import org.jetbrains.kotlin.js.engine.ScriptExecutionException
import org.jetbrains.kotlin.test.GroupingStageInputsHolder
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import org.jetbrains.kotlin.test.model.JsIrArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertNull

class JsGroupingStageBoxRunnerTest {
    @Test
    fun testsNotReachedInCrashedModeAreFailed() {
        val reached = GroupedTest("testReached")
        val crasher = GroupedTest("testCrasher")
        val notReached = GroupedTest("testNotReached")
        val batch = listOf(reached, crasher, notReached)

        val completedOutput = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            for (test in batch) {
                appendResultLine(test, GroupedTestsResultProtocol.STARTED)
                appendResultLine(test, GroupedTestsResultProtocol.PASSED)
            }
            appendLine(GroupedTestsResultProtocol.END)
        }
        val crashedOutput = buildString {
            appendLine(GroupedTestsResultProtocol.BEGIN)
            appendResultLine(reached, GroupedTestsResultProtocol.STARTED)
            appendResultLine(reached, GroupedTestsResultProtocol.PASSED)
            appendResultLine(crasher, GroupedTestsResultProtocol.STARTED)
        }

        runBatch(
            batch,
            Execution(COMPLETED_MODE, completedOutput, vmFailure = null),
            Execution(CRASHED_MODE, crashedOutput, ScriptExecutionException(crashedOutput, "Fatal JavaScript out of memory")),
        )

        assertNull(reached.failure, "A test that passed in every mode was failed")
        assertContains(crasher.failure?.message.orEmpty(), "The test started but reported no result in $CRASHED_MODE")
        assertContains(notReached.failure?.message.orEmpty(), "The test reported no result in $CRASHED_MODE.")
    }

    private class Execution(val name: String, val output: String, val vmFailure: ScriptExecutionException?)

    private class GroupedTest(methodName: String) {
        private val failures = mutableListOf<Throwable>()

        val input = NonGroupingStageOutput(
            testServices = TestServices().apply {
                register(KotlinTestInfo::class, KotlinTestInfo(className = "JsGroupedTests", methodName = methodName, tags = emptySet()))
            },
            catchingExecutor = { _, block ->
                try {
                    block()
                } catch (e: Throwable) {
                    failures += e
                }
            },
        )

        val id: String = computeJsGroupedTestId(input.testInfo)

        val failure: Throwable?
            get() = failures.singleOrNull().also { check(failures.size <= 1) { "$id was failed more than once: $failures" } }
    }

    private fun runBatch(batch: List<GroupedTest>, vararg executions: Execution) {
        val testServices = TestServices().apply {
            register(AssertionsService::class, JUnit5Assertions)
            register(GroupingStageInputsHolder::class, GroupingStageInputsHolder(batch.map { it.input }))
        }
        val runner = object : JsGroupingStageBoxRunner(testServices) {
            override fun executeGroupedBatch(artifact: JsGroupedBatchArtifact, inputs: List<NonGroupingStageOutput>) =
                executions.map { GroupedBatchExecution(it.name, it.output, it.vmFailure) }
        }
        val launcherModule = TestModule(
            name = "launcher",
            files = emptyList(),
            allDependencies = emptyList(),
            directives = RegisteredDirectives.Empty,
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        )
        runner.processArtifact(JsGroupedBatchArtifact(JsIrArtifact(File("launcher.js"), CompilerResult(emptyMap())), launcherModule))
    }

    private fun StringBuilder.appendResultLine(test: GroupedTest, status: String) {
        appendLine(listOf(GroupedTestsResultProtocol.LINE_PREFIX, test.id, status, "", "").joinToString(GroupedTestsResultProtocol.SEP))
    }

    private companion object {
        const val COMPLETED_MODE = "FULL_DEV"
        const val CRASHED_MODE = "FULL_PROD_MINIMIZED_NAMES"
    }
}

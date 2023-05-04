/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "ThrowableNotThrown")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.ProjectConfigurationException
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.IllegalLifecycleException
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.ProjectConfigurationResult.Failure
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.*
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.tooling.core.withClosure
import org.jetbrains.kotlin.tooling.core.withLinearClosure
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*

class KotlinPluginLifecycleTest {

    private val project = buildProjectWithMPP()
    private val lifecycle = project.kotlinPluginLifecycle as KotlinPluginLifecycleImpl

    @Test
    fun `test - configure phase is executed right away`() {
        val invocations = AtomicInteger(0)
        lifecycle.enqueue(EvaluateBuildscript) {
            invocations.incrementAndGet()
        }
        assertEquals(1, invocations.get(), "Expected one invocation")
    }

    @Test
    fun `test - launchInState - Configure`() {
        val invocations = AtomicInteger(0)
        project.launchInStage(EvaluateBuildscript) {
            assertEquals(EvaluateBuildscript, stage)
            assertEquals(1, invocations.incrementAndGet())
        }
        assertEquals(1, invocations.get())
    }

    @Test
    fun `test - configure phase - nested enqueue - is executed as queue`() {
        val outerInvocations = AtomicInteger(0)
        val nestedAInvocations = AtomicInteger(0)
        val nestedBInvocations = AtomicInteger(0)
        val nestedCInvocations = AtomicInteger(0)
        lifecycle.enqueue(EvaluateBuildscript) {
            assertEquals(1, outerInvocations.incrementAndGet())

            lifecycle.enqueue(EvaluateBuildscript) nestedA@{
                assertEquals(0, nestedBInvocations.get(), "Expected nestedA to be executed before nestedB")
                assertEquals(1, nestedAInvocations.incrementAndGet())

                lifecycle.enqueue(EvaluateBuildscript) nestedC@{
                    assertEquals(1, nestedBInvocations.get(), "Expected nestedB to be executed before nestedC")
                    assertEquals(1, nestedCInvocations.incrementAndGet())
                }
            }

            lifecycle.enqueue(EvaluateBuildscript) nestedB@{
                assertEquals(1, nestedAInvocations.get(), "Expected nestedA to be executed before nestedB")
                assertEquals(0, nestedCInvocations.get(), "Expected nestedB to be executed before nestedC")
                assertEquals(1, nestedBInvocations.incrementAndGet())
            }
        }

        assertEquals(1, outerInvocations.get())
        assertEquals(1, nestedAInvocations.get())
        assertEquals(1, nestedBInvocations.get())
        assertEquals(1, nestedCInvocations.get())
    }

    @Test
    fun `test - all stages are executed in order`() {
        val invocations = Stage.values().associateWith { AtomicInteger(0) }
        Stage.values().toList().forEach { stage ->
            lifecycle.enqueue(stage) {
                Stage.values().forEach { otherStage ->
                    when {
                        otherStage.ordinal < stage.ordinal -> assertEquals(1, invocations.getValue(otherStage).get())
                        otherStage.ordinal == stage.ordinal -> assertEquals(1, invocations.getValue(stage).incrementAndGet())
                        else -> assertEquals(0, invocations.getValue(otherStage).get())
                    }
                }
            }
        }

        project.evaluate()

        invocations.forEach { (stage, invocations) ->
            assertEquals(1, invocations.get(), "Expected stage '$stage' to be executed")
        }
    }

    @Test
    fun `test - afterEvaluate based stage executes queue in order`() {
        val action1Invocations = AtomicInteger(0)
        val action2Invocations = AtomicInteger(0)
        val action3Invocations = AtomicInteger(0)

        lifecycle.enqueue(ReadyForExecution) action3@{
            assertEquals(1, action1Invocations.get(), "Expected action1 to be executed before action3")
            assertEquals(1, action2Invocations.get(), "Expected action2 to be executed before action3")
            assertEquals(1, action3Invocations.incrementAndGet())
        }

        lifecycle.enqueue(AfterEvaluateBuildscript) action1@{
            assertEquals(0, action2Invocations.get(), "Expected action1 to be executed before action2")
            assertEquals(0, action3Invocations.get(), "Expected action1 to be executed before action3")
            assertEquals(1, action1Invocations.incrementAndGet())
        }

        lifecycle.enqueue(AfterEvaluateBuildscript) action2@{
            assertEquals(1, action1Invocations.get(), "Expected action1 to be executed before action2")
            assertEquals(0, action3Invocations.get(), "Expected action2 to be executed before action3")
            assertEquals(1, action2Invocations.incrementAndGet())
        }

        assertEquals(0, action1Invocations.get())
        assertEquals(0, action2Invocations.get())
        assertEquals(0, action3Invocations.get())

        project.evaluate()

        assertEquals(1, action1Invocations.get())
        assertEquals(1, action2Invocations.get())
        assertEquals(1, action3Invocations.get())
    }

    @Test
    fun `test - afterEvaluate based stage - allows enqueue in current stage`() {
        val outerInvocations = AtomicInteger(0)
        val nestedAInvocations = AtomicInteger(0)
        val nestedBInvocations = AtomicInteger(0)
        val nestedCInvocations = AtomicInteger(0)
        lifecycle.enqueue(AfterEvaluateBuildscript) {
            assertEquals(1, outerInvocations.incrementAndGet())

            lifecycle.enqueue(AfterEvaluateBuildscript) nestedA@{
                assertEquals(0, nestedBInvocations.get(), "Expected nestedA to be executed before nestedB")
                assertEquals(1, nestedAInvocations.incrementAndGet())

                lifecycle.enqueue(AfterEvaluateBuildscript) nestedC@{
                    assertEquals(1, nestedBInvocations.get(), "Expected nestedB to be executed before nestedC")
                    assertEquals(1, nestedCInvocations.incrementAndGet())
                }
            }

            lifecycle.enqueue(AfterEvaluateBuildscript) nestedB@{
                assertEquals(1, nestedAInvocations.get(), "Expected nestedA to be executed before nestedB")
                assertEquals(0, nestedCInvocations.get(), "Expected nestedB to be executed before nestedC")
                assertEquals(1, nestedBInvocations.incrementAndGet())
            }
        }

        assertEquals(0, outerInvocations.get())
        assertEquals(0, nestedAInvocations.get())
        assertEquals(0, nestedBInvocations.get())
        assertEquals(0, nestedCInvocations.get())

        project.evaluate()

        assertEquals(1, outerInvocations.get())
        assertEquals(1, nestedAInvocations.get())
        assertEquals(1, nestedBInvocations.get())
        assertEquals(1, nestedCInvocations.get())
    }

    @Test
    fun `test - enqueue of already executed stage - throws exception`() {
        val executed = AtomicBoolean(false)
        lifecycle.enqueue(ReadyForExecution) {
            assertFailsWith<IllegalLifecycleException> {
                lifecycle.enqueue(AfterEvaluateBuildscript) { fail("This code shall not be executed!") }
            }
            assertFalse(executed.getAndSet(true))
        }

        project.evaluate()
        assertTrue(executed.get())
    }

    @Test
    fun `test - throwing an exception during buildscript evaluation - shall not execute afterEvaluate based stages`() {
        val executed = AtomicReference<Throwable>()
        lifecycle.enqueue(AfterEvaluateBuildscript) {
            assertNull(executed.getAndSet(Throwable()))
        }

        val thrownException = Exception()

        /* Provoking an exception during project.evaluate() */
        val exceptionWasProvoked = AtomicBoolean()
        project.tasks.whenObjectAdded {
            assertFalse(exceptionWasProvoked.getAndSet(true))
            throw thrownException
        }
        runCatching { project.evaluate() }
        assertTrue(exceptionWasProvoked.get(), "Exception during '.evaluate()' was not provoked")

        /* Assert: The 'AfterEvaluate' based stage should not have been executed */
        executed.get()?.let { throwable ->
            fail("AfterEvaluate based stage is not expected to be launched because of exception during .evaluate()", throwable)
        }

        val exceptions = project.configurationResult.getOrThrow().cast<Failure>()
            .failures.withClosure<Throwable> { listOfNotNull(it.cause) }
        if (thrownException !in exceptions) fail("Expected 'thrownException' in lifecycle.finished")
    }

    @Test
    fun `test - throwing an exception during buildscript evaluation - will execute coroutine waiting for finished`() {
        val thrownException = Exception()

        val executedAfterLifecycleFinished = AtomicBoolean(false)
        project.launch {
            val exceptions = project.configurationResult.await().cast<Failure>()
                .failures.withClosure<Throwable> { listOfNotNull(it.cause) }
            assertFalse(executedAfterLifecycleFinished.getAndSet(true))
            if (thrownException !in exceptions) fail("Expected 'thrownException' in lifecycle.finished")
        }


        /* Provoking an exception during project.evaluate() */
        val exceptionWasProvoked = AtomicBoolean()
        project.tasks.whenObjectAdded {
            assertFalse(exceptionWasProvoked.getAndSet(true))
            throw thrownException
        }
        runCatching { project.evaluate() }
        assertTrue(exceptionWasProvoked.get(), "Exception during '.evaluate()' was not provoked")
        assertTrue(executedAfterLifecycleFinished.get(), "Expected coroutine waiting for '.finished' to be executed")
    }

    @Test
    fun `test - throwing an exception in afterEvaluate based stage - allows getOrThrow on future`() {
        val exception = IllegalStateException()
        val future = project.future { AfterEvaluateBuildscript.await(); 42 }
        val secondActionExecuted = AtomicBoolean(false)

        project.launchInStage(AfterEvaluateBuildscript) {
            throw exception
        }

        project.launch secondAction@{
            project.configurationResult.await()
            assertEquals(AfterEvaluateBuildscript, stage)
            assertEquals(42, future.getOrThrow())
            assertEquals(420, project.future { AfterEvaluateBuildscript.await(); 420 }.getOrThrow())
            assertFalse(secondActionExecuted.getAndSet(true))
        }

        assertTrue(exception in assertFails { project.evaluate() }.withLinearClosure { it.cause })
        assertTrue(secondActionExecuted.get())
    }

    @Test
    fun `test - throwing an exception in afterEvaluate based stage - will not execute coroutines in later stages`() {
        val exception = IllegalStateException()

        val secondActionExecuted = AtomicBoolean(false)
        val thirdActionExecuted = AtomicBoolean(false)
        val fourthActionExecuted = AtomicBoolean(false)

        project.launchInStage(AfterEvaluateBuildscript) {
            throw exception
        }

        project.launchInStage(AfterEvaluateBuildscript.nextOrThrow) secondAction@{
            assertFalse(secondActionExecuted.getAndSet(true))
        }

        project.launch {
            project.configurationResult.await()
            project.launchInStage(AfterEvaluateBuildscript.nextOrThrow) thirdAction@{
                assertFalse(thirdActionExecuted.getAndSet(true))
            }
        }

        project.launch fourthAction@{
            project.configurationResult.await()
            AfterEvaluateBuildscript.nextOrThrow.await()
            assertFalse(fourthActionExecuted.getAndSet(true))
        }

        val failure = assertFails { project.evaluate() }

        assertTrue(
            exception in failure.withLinearClosure { it.cause },
            "Could not find 'exception' in failure cause\n${failure.stackTraceToString()}",
        )

        assertFalse(secondActionExecuted.get())
        assertFalse(thirdActionExecuted.get())
        assertFalse(fourthActionExecuted.get())
    }

    @Test
    fun `test - stage property is correct`() {
        Stage.values().forEach { stage ->
            lifecycle.enqueue(stage) {
                assertEquals(lifecycle.stage, stage)
            }
        }
        project.evaluate()
    }

    @Test
    fun `test - invoke configure twice`() {
        val action1Invocations = AtomicInteger(0)
        val action2Invocations = AtomicInteger(0)
        val action3Invocations = AtomicInteger(0)

        lifecycle.enqueue(EvaluateBuildscript) action1@{
            assertEquals(0, action2Invocations.get())
            assertEquals(0, action3Invocations.get())
            assertEquals(1, action1Invocations.incrementAndGet())
        }

        lifecycle.enqueue(EvaluateBuildscript) action2@{
            lifecycle.enqueue(EvaluateBuildscript) action3@{
                assertEquals(1, action1Invocations.get())
                assertEquals(1, action2Invocations.get())
                assertEquals(1, action3Invocations.incrementAndGet())
            }

            assertEquals(1, action1Invocations.get())
            assertEquals(0, action3Invocations.get())
            assertEquals(1, action2Invocations.incrementAndGet())
        }

        assertEquals(1, action1Invocations.get())
        assertEquals(1, action2Invocations.get())
        assertEquals(1, action3Invocations.get())
    }

    @Test
    fun `test - launch in configure`() {
        val action1Invocations = AtomicInteger(0)
        val action2Invocations = AtomicInteger(0)
        val action3Invocations = AtomicInteger(0)

        lifecycle.launch action1@{
            lifecycle.launch action2@{
                assertEquals(1, action1Invocations.get())
                assertEquals(0, action3Invocations.get())
                assertEquals(1, action2Invocations.incrementAndGet())
            }
            assertEquals(0, action2Invocations.get())
            assertEquals(0, action3Invocations.get())
            assertEquals(1, action1Invocations.incrementAndGet())

            lifecycle.launch action3@{
                assertEquals(1, action1Invocations.get())
                assertEquals(1, action2Invocations.get())
                assertEquals(1, action3Invocations.incrementAndGet())
            }
        }

        assertEquals(1, action1Invocations.get())
        assertEquals(1, action2Invocations.get())
        assertEquals(1, action3Invocations.get())
    }

    @Test
    fun `test - launch in configure - await Stage`() {
        val executionPointA = AtomicBoolean(false)
        val executionPointB = AtomicBoolean(false)
        lifecycle.launch action1@{
            assertFalse(executionPointA.getAndSet(true))
            assertEquals(EvaluateBuildscript, stage)
            await(AfterEvaluateBuildscript)
            assertEquals(AfterEvaluateBuildscript, stage)
            assertFalse(executionPointB.getAndSet(true))
        }

        assertTrue(executionPointA.get())
        assertFalse(executionPointB.get())
        project.evaluate()
        assertTrue(executionPointA.get())
        assertTrue(executionPointB.get())
    }

    @Test
    fun `test - launch - await - launch`() {
        val executedInnerAction = AtomicBoolean(false)
        lifecycle.launch {
            await(AfterEvaluateBuildscript)
            launch {
                assertEquals(AfterEvaluateBuildscript, stage)
                await(FinaliseRefinesEdges)
                assertEquals(FinaliseRefinesEdges, stage)
                assertFalse(executedInnerAction.getAndSet(true))
            }
        }

        assertFalse(executedInnerAction.get())
        project.evaluate()
        assertTrue(executedInnerAction.get())
    }

    @Test
    fun `test - launch - exception`() {
        assertFailsWith<IllegalStateException> {
            lifecycle.launch {
                throw IllegalStateException("42")
            }
        }
    }

    @Test
    fun `test - launch - await - exception`() {
        val testException = object : Throwable() {}
        lifecycle.launch {
            await(AfterEvaluateBuildscript)
            launch {
                throw testException
            }
        }

        val causes = assertFailsWith<ProjectConfigurationException> {
            project.evaluate()
        }.withLinearClosure<Throwable> { it.cause }

        assertTrue(testException in causes)
    }

    @Test
    fun `test - require current stage`() = project.runLifecycleAwareTest {
        launchInStage(AfterEvaluateBuildscript) {
            requireCurrentStage { } // OK

            requireCurrentStage {
                /* Fails because of stage transition  using 'await' */
                assertFailsWith<IllegalLifecycleException> { await(ReadyForExecution) }
            }
        }
    }

    @Test
    fun `test - launch in required stage`() = project.runLifecycleAwareTest {
        launchInRequiredStage(AfterEvaluateBuildscript) {
            assertEquals(AfterEvaluateBuildscript, stage)
            await(AfterEvaluateBuildscript)
            assertEquals(AfterEvaluateBuildscript, stage)
            assertFailsWith<IllegalLifecycleException> { await(ReadyForExecution) }
        }
    }

    @Test
    fun `test - withRestrictedStages`() = project.runLifecycleAwareTest {
        launch {
            withRestrictedStages(Stage.upTo(FinaliseRefinesEdges)) {
                assertEquals(EvaluateBuildscript, stage)

                await(AfterEvaluateBuildscript)
                assertEquals(AfterEvaluateBuildscript, stage)

                await(FinaliseDsl)
                assertEquals(FinaliseDsl, stage)

                await(FinaliseRefinesEdges)
                assertEquals(FinaliseRefinesEdges, stage)

                assertFailsWith<IllegalLifecycleException> {
                    await(AfterFinaliseRefinesEdges)
                }
            }
        }
    }

    @Test
    fun `test - launching in AfterEvaluate`() = project.runLifecycleAwareTest {
        val actionInvocations = AtomicInteger(0)

        afterEvaluate {
            launch {
                assertEquals(AfterEvaluateBuildscript.nextOrThrow, currentKotlinPluginLifecycle().stage)
                assertEquals(1, actionInvocations.incrementAndGet())
            }
        }

        AfterEvaluateBuildscript.nextOrThrow.nextOrThrow.await()
        assertEquals(1, actionInvocations.get())
        Stage.values.last().await()
    }

    /**
     * This requirement is important to safely support project.future { }.getOrThrow() patterns (when the lifecycle is finished),
     */
    @Test
    fun `test - launching after Lifecycle finished - will execute code right away`() {
        project.evaluate()
        val actionAInvocations = AtomicInteger(0)
        val actionBInvocations = AtomicInteger(0)
        project.launch actionB@{
            project.launch actionA@{
                assertEquals(0, actionBInvocations.get())
                assertEquals(1, actionAInvocations.incrementAndGet())
            }

            assertEquals(1, actionAInvocations.get())
            assertEquals(1, actionBInvocations.incrementAndGet())

        }
        assertEquals(1, actionAInvocations.get())
        assertEquals(1, actionBInvocations.get())
    }

    @Test
    fun `test - Stage - previous next utils`() {
        assertNull(Stage.values.first().previousOrNull)
        assertEquals(Stage.values.first(), Stage.values.first().previousOrFirst)

        assertNull(Stage.values.last().nextOrNull)
        assertEquals(Stage.values.last(), Stage.values.last().nextOrLast)

        assertFailsWith<IllegalArgumentException> { Stage.values.last().nextOrThrow }
    }

    @Test
    fun `test - Stage - range utils`() {
        assertEquals(setOf(AfterEvaluateBuildscript, FinaliseDsl, AfterFinaliseDsl), AfterEvaluateBuildscript..AfterFinaliseDsl)
        assertEquals(emptySet(), AfterFinaliseDsl..AfterFinaliseDsl.previousOrThrow)
        assertEquals(setOf(FinaliseDsl), FinaliseDsl..FinaliseDsl)

        assertTrue(FinaliseDsl in Stage.upTo(FinaliseDsl))
        assertTrue(Stage.values.first() in Stage.upTo(FinaliseDsl))
        assertTrue(FinaliseDsl.previousOrThrow in Stage.upTo(FinaliseDsl))
        assertTrue(FinaliseDsl.nextOrThrow !in Stage.upTo(FinaliseDsl))

        assertTrue(FinaliseDsl !in Stage.until(FinaliseDsl))
        assertTrue(FinaliseDsl.previousOrFirst in Stage.until(FinaliseDsl))
    }
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.IllegalLifecycleException
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.ProjectConfigurationResult
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.*
import org.jetbrains.kotlin.gradle.util.assertIsInstance
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.utils.future
import kotlin.test.*

/**
 * This class is not containing tests designed as 'unit tests', with proper assertions and quality control in mind
 * (See [KotlinPluginLifecycleTest] for that).
 *
 * However, this class is designed to provide readable samples to showcase the behaviour and usage of the KotlinPluginLifecycle
 */
class KotlinPluginLifecycleSample {

    /**
     * Launching in 'EvaluateBuildscript' will execute the launched code right away!
     * This code will showcase that launching before any 'afterEvaluate' listeners have been invoked is possible.
     * However, the launched coroutines can be executed right away.
     */
    @Test
    fun `launch in EvaluateBuildscript`() {
        val project = buildProject()
        project.startKotlinPluginLifecycle()

        var executed = false

        /* Project is not yet in 'afterEvaluate' */
        project.launch {
            executed = true
        }

        assertTrue(executed, "Expected coroutine in $EvaluateBuildscript Phase to be executed right away")
    }

    /**
     * Launching in code 'AfterEvaluateBuildscript' Stage:
     * This sample shows how code can be deferred into a 'afterEvaluate' based Stage.
     * Using [launchInStage]: This code will only be executed once the stage will be reached.
     */
    @Test
    fun `launchInStage AfterEvaluate`() {
        val project = buildProject()
        project.startKotlinPluginLifecycle()

        var executed = false

        project.launchInStage(AfterEvaluateBuildscript) {
            executed = true
        }

        assertFalse(executed, "Expected coroutine in $AfterEvaluateBuildscript not to be executed right away")

        /* Call to project.evaluate() will mimic buildscript evaluation and calls into all afterEvaluate listeners from Gradle */
        project.evaluate()

        /* Coroutine launch was executed now */
        assertTrue(executed, "Expected coroutine in $AfterEvaluateBuildscript to be executed in 'afterEvaluate'")
    }

    /**
     * Similar to [launchInStage AfterEvaluate], but shows launching in a later stage ([FinaliseDsl]).
     * Showcases that [launchInStage] will only execute the code once the respective Stage was reached.
     */
    @Test
    fun `launchInStage FinaliseDsl`() {
        val project = buildProject()
        project.startKotlinPluginLifecycle()

        project.launchInStage(FinaliseDsl) {
            /* launchInStage will run the given coroutine within the provided Stage */
            assertEquals(FinaliseDsl, project.kotlinPluginLifecycle.stage)
        }

        project.evaluate()
    }

    /**
     * Shows how a stage like [FinaliseDsl] can be awaited using [await]:
     * A coroutine using [await] will suspend the execution until this [KotlinPluginLifecycle.Stage] was reached.
     * Note: The semantics is 'the stage was reached' **not** 'the stage was completed'
     */
    @Test
    fun `await FinaliseDsl Stage in coroutine`() {
        val project = buildProject()
        project.startKotlinPluginLifecycle()

        project.launch {
            /* The coroutine is launched within EvaluateBuildscript */
            assertEquals(KotlinPluginLifecycle.Stage.EvaluateBuildscript, project.kotlinPluginLifecycle.stage)

            /* Suspending execution until 'FinaliseDsl' arrived */
            FinaliseDsl.await()

            /* The current stage is still 'FinaliseDsl' */
            assertEquals(KotlinPluginLifecycle.Stage.FinaliseDsl, project.kotlinPluginLifecycle.stage)
        }

        project.evaluate()
    }

    /**
     * Showcase of error handling when there was an error thrown within the build.gradle.kts file (or in any plugin.apply())
     * Example would be:
     *
     * build.gradle.kts
     * ```kotlin
     * kotlin {
     *     sourceSets.getByName("nonExistentSourceSet") // <- throws UnknownDomainObjectException
     * }
     * ```
     */
    @Test
    fun `exception thrown in buildscript evaluation - inside coroutine`() {
        /*
        Example: Error thrown in a launch in 'EvaluateBuildscript'
         */

        val project = buildProjectWithMPP()
        val executed = mutableListOf<String>()

        /* TestException thrown in belows launch will be propagated here */
        assertFailsWith<TestException> {
            project.launch {
                executed.add("first")
            }

            project.launch {
                executed.add("second")
                throw TestException()
            }

            project.launch {
                executed.add("third") // <- Will not get executed!
            }
        }

        assertEquals(listOf("first", "second"), executed)

        /* Showcase of which futures will be available */
        run {
            /* Future that only requires 'BuildscriptEvaluation' is available */
            assertNotNull(project.future { KotlinPluginLifecycle.Stage.EvaluateBuildscript.await() }.getOrThrow())

            /* Future that requires 'AfterEvaluateBuildscript' is not available */
            assertFailsWith<IllegalLifecycleException> {
                project.future { KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript.await() }.getOrThrow()
            }
        }
    }

    /**
     * Demonstrates how coroutines will behave if any exception is thrown within the buildscript of the user.
     * Such an exception would be:
     * ```kotlin
     * kotlin {
     *     sourceSets.getByName("notExistingSourceSet") // <- throws UnknownDomainObjectException
     * }
     * ``
     *
     * In this case, all coroutines scheduled for later execution *will not be executed*
     */
    @Test
    fun `exception thrown in buildscript evaluation - inside user buildscript`() {
        val project = buildProjectWithMPP()
        val executed = mutableListOf<String>()

        project.launchInStage(AfterEvaluateBuildscript) {
            /*
            Will not get executed as this Stage will never be reached.
            Lifecycle finishes in failure state before!
             */
            executed.add("AfterEvaluateBuildscript")
        }

        project.launchInStage(ReadyForExecution) {
            /*
            Will not get executed as this Stage will never be reached.
            Lifecycle finishes in failure state before!
             */
            executed.add("ReadyForExecution")
        }

        /*
        Forcing project.evaluate() to throw an exception:
        Trust me, this will mimic any error in users buildscripts sufficiently!
        */
        project.tasks.whenObjectAdded { throw TestException() }
        assertFails { project.evaluate() }

        /* Project has failed: Lifecycle finished. We can still access futures! */
        run {
            /* Failure state is still in 'EvaluateBuildscript' Stage */
            assertEquals(EvaluateBuildscript, project.kotlinPluginLifecycle.stage)

            /* None of the coroutines before was executed */
            assertEquals(emptyList(), executed)

            /* Access configurationResult future as .getOrThrow */
            val result = project.configurationResult.getOrThrow()
            assertIsInstance<ProjectConfigurationResult.Failure>(result)

            /* Access configurationResult future as .future {} */
            project.future { project.configurationResult.await() }.getOrThrow()
        }
    }

    /**
     * Showcases how coroutines will behave if there is an exception thrown within the buildscript of the user.
     * In particular this sample shows how calls to `project.configurationResult` will be handled!
     *
     * In short: All coroutines that already suspended, waiting for the configurationResult will be unsuspended.
     * Like in [exception thrown in buildscript evaluation - inside user buildscript]:
     * Coroutines that ware waiting for later 'Stages' will not be executed (as their requirements are unmet)
     */
    @Test
    fun `exception thrown in buildscript evaluation - coroutines waiting for configurationResult`() {
        val project = buildProjectWithMPP()
        val executed = mutableListOf<String>()

        project.launch {
            project.configurationResult.await()
            executed.add("configurationResult.await()")
        }

        project.launch {
            FinaliseDsl.await() // <- This coroutine requires 'FinaliseDsl' in order to continue
            project.configurationResult.await() // <- Only then it will be applicable for configurationResult
            executed.add("FinaliseDsl") // <- Will never be executed because of exception thrown 'earlier'
        }

        project.launchInStage(AfterEvaluateBuildscript) {
            executed.add("AfterEvaluateBuildscript") // <- Will never be executed befause of exception thrown 'earlier'
        }

        project.tasks.whenObjectAdded { throw TestException() }
        assertFails { project.evaluate() }

        /* We never moved outside the EvaluateBuildscript Stage */
        assertEquals(EvaluateBuildscript, project.kotlinPluginLifecycle.stage)
        assertEquals(listOf("configurationResult.await()"), executed)
    }

    /**
     * Sample showcases how an exception thrown within a stage like [AfterEvaluateBuildscript] is handled:
     * All coroutines scheduled after the throwing coroutine will not be executed!
     * Coroutines already waiting for the projects configuration result will be unsuspended.
     *
     * Coroutines launched *after* the failure state has reached will be launched w/o suspensions.
     */
    @Test
    fun `exception thrown in AfterEvaluateBuildscript`() {
        val project = buildProjectWithMPP()
        val executed = mutableListOf<String>()

        project.launchInStage(AfterEvaluateBuildscript) {
            executed.add("AfterEvaluateBuildscript 1")
        }

        project.launchInStage(AfterEvaluateBuildscript) {
            executed.add("AfterEvaluateBuildscript 2")
            throw TestException()
        }

        project.launchInStage(AfterEvaluateBuildscript) {
            executed.add("AfterEvaluateBuildscript 3")
        }

        project.launchInStage(ReadyForExecution) {
            executed.add("ReadyForExecution")
        }

        assertFails { project.evaluate() }

        /* Showcase of which coroutines were executed */
        run {
            /* We failed in 'AfterEvaluateBuildscript' */
            assertEquals(AfterEvaluateBuildscript, project.kotlinPluginLifecycle.stage)

            /* Any coroutine scheduled for 'running after 'AfterEvaluateBuildscript 2' was not executed! */
            assertEquals(listOf("AfterEvaluateBuildscript 1", "AfterEvaluateBuildscript 2"), executed)

            /* ProjectConfigurationResult is available */
            assertIsInstance<ProjectConfigurationResult.Failure>(project.configurationResult.getOrThrow())
        }

        /* Showcase of which futures will be available */
        run {
            /* Future that only requires 'BuildscriptEvaluation' is available */
            assertNotNull(project.future { KotlinPluginLifecycle.Stage.EvaluateBuildscript.await() }.getOrThrow())

            /*
            Future that requires 'AfterEvaluateBuildscript' is available
            Why will this future be returning?
            Because the semantics of 'KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript.await()' is
            "I require this stage to execute" and not "I require this stage to be finished".
            So even if there formally wy an Exception thrown within the Stage, the requirement for this future
            will be met: We indeed made it into this Stage.
            */
            assertNotNull(project.future { KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript.await() }.getOrThrow())

            /* Future that requires 'ReadyForExecution' is not available */
            assertFailsWith<IllegalLifecycleException> {
                project.future { KotlinPluginLifecycle.Stage.ReadyForExecution.await() }.getOrThrow()
            }
        }
    }

    /**
     * Sample showcasing how one can wait for the finished project configurationResult when no exception is reached.
     * Just simply awaiting the future is enough here!
     * In happy case the stage will be [ReadyForExecution]
     */
    @Test
    fun `awaiting Project configurationResult`() {
        val project = buildProjectWithMPP()
        project.launch {
            /* This coroutine is executed right away in EvaluateBuildscript */
            assertEquals(EvaluateBuildscript, project.kotlinPluginLifecycle.stage)

            /* Suspension point: We are waiting for lifecycle/project configuration to finish fully */
            val result = project.configurationResult.await()
            assertIsInstance<ProjectConfigurationResult.Success>(result)

            /* Since no errors have been thrown: ReadyForExecution Stage has been reached */
            assertEquals(ReadyForExecution, project.kotlinPluginLifecycle.stage)
        }
        project.evaluate()
    }

    /**
     * Showcases how coroutines will be treated if there are waiting for [configurationResult], but
     * an exception is thrown in a intermediate [KotlinPluginLifecycle.Stage]
     *
     * Coroutines that already suspended, waiting for the result will be unsuspended.
     * Coroutines that are in the queue when the exception is thrown will not be executed anymore.
     */
    @Test
    fun `awaiting Project configurationResult - with error thrown in FinaliseDsl`() {
        val project = buildProjectWithMPP()
        val executed = mutableListOf<String>()

        project.launch {
            executed.add("launch:beforeConfigurationResult")
            val result = project.configurationResult.await()
            assertIsInstance<ProjectConfigurationResult.Failure>(result)
            executed.add("launch:afterConfigurationResult")
        }

        project.launchInStage(FinaliseDsl) {
            executed.add("FinaliseDsl:beforeException")

            val result = project.configurationResult.await()
            assertIsInstance<ProjectConfigurationResult.Failure>(result)
            executed.add("FinaliseDsl:beforeException:afterConfigurationResult")
        }

        project.launchInStage(FinaliseDsl) {
            executed.add("FinaliseDsl:exception")
            throw TestException()
        }

        project.launchInStage(FinaliseDsl) {
            /* Never executed because previous launch in stage threw exception */
            executed.add("FinaliseDsl:afterException")
        }

        assertFails { project.evaluate() }

        assertEquals(
            listOf(
                "launch:beforeConfigurationResult", //  First: 'launched'/'enqueued' as first
                "FinaliseDsl:beforeException", // Second: 'launched' before the coroutine that throws exception
                "FinaliseDsl:exception", // Third: 'launched' after 'FinaliseDsl:beforeException' coroutine
                "launch:afterConfigurationResult", // Gets result first: suspended first!
                "FinaliseDsl:beforeException:afterConfigurationResult" // Gets result second: suspended second!
            ), executed
        )
    }

    private class TestException : Exception()
}

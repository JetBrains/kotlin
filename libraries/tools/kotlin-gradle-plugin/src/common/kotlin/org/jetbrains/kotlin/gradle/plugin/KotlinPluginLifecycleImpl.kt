/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.CoroutineStart.Default
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.CoroutineStart.Undispatched
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.ProjectConfigurationResult
import org.jetbrains.kotlin.gradle.utils.whenEvaluated
import org.jetbrains.kotlin.gradle.utils.CompletableFuture
import org.jetbrains.kotlin.gradle.utils.failures
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.*

internal val CoroutineContext.kotlinPluginLifecycle: KotlinPluginLifecycle
    get() = this[KotlinPluginLifecycleCoroutineContextElement]?.lifecycle
        ?: error("Missing $KotlinPluginLifecycleCoroutineContextElement in currentCoroutineContext")


internal class KotlinPluginLifecycleImpl(override val project: Project) : KotlinPluginLifecycle {
    val configurationResult = CompletableFuture<ProjectConfigurationResult>()

    private val enqueuedActions: Map<KotlinPluginLifecycle.Stage, ArrayDeque<KotlinPluginLifecycle.() -> Unit>> =
        KotlinPluginLifecycle.Stage.values().associateWith { ArrayDeque() }

    private val loopRunning = AtomicBoolean(false)
    private val isStartedImpl = AtomicBoolean(false)
    private val isFinishedSuccessfully = AtomicBoolean(false)
    private val isFinishedWithFailures = AtomicBoolean(false)


    override var stage: KotlinPluginLifecycle.Stage = KotlinPluginLifecycle.Stage.values.first()

    override val isStarted: Boolean
        get() = isStartedImpl.get()

    override val isFinished: Boolean
        get() = isFinishedSuccessfully.get() || isFinishedWithFailures.get()

    fun start() {
        check(!isStartedImpl.getAndSet(true)) {
            "${KotlinPluginLifecycle::class.java.name} already started"
        }

        check(!project.state.executed) {
            "${KotlinPluginLifecycle::class.java.name} cannot be started in ProjectState '${project.state}'"
        }

        loopIfNecessary()

        project.whenEvaluated {
            /* Check for failures happening during buildscript evaluation */
            project.failures.let { failures ->
                if (failures.isNotEmpty()) {
                    finishWithFailures(failures)
                    return@whenEvaluated
                }
            }

            assert(enqueuedActions.getValue(stage).isEmpty()) { "Expected empty queue from '$stage'" }
            stage = stage.nextOrThrow
            executeCurrentStageAndScheduleNext()
        }
    }

    private fun executeCurrentStageAndScheduleNext() {
        stage.previousOrNull?.let { previousStage ->
            assert(enqueuedActions.getValue(previousStage).isEmpty()) {
                "Actions from previous stage '$previousStage' have not been executed (stage: '$stage')"
            }
        }

        val failures = project.failures
        if (failures.isNotEmpty()) {
            finishWithFailures(failures)
            return
        }

        try {
            loopIfNecessary()
        } catch (t: Throwable) {
            finishWithFailures(listOf(t))
            throw t
        }

        stage = stage.nextOrNull ?: run {
            finishSuccessfully()
            return
        }

        project.afterEvaluate {
            executeCurrentStageAndScheduleNext()
        }
    }

    private fun loopIfNecessary() {
        if (loopRunning.getAndSet(true)) return
        try {
            val queue = enqueuedActions.getValue(stage)
            do {
                project.state.rethrowFailure()
                val action = queue.removeFirstOrNull()
                action?.invoke(this)
            } while (action != null)
        } finally {
            loopRunning.set(false)
        }
    }

    private fun finishWithFailures(failures: List<Throwable>) {
        assert(failures.isNotEmpty())
        assert(isStartedImpl.get())
        assert(!isFinishedWithFailures.getAndSet(true))
        configurationResult.complete(ProjectConfigurationResult.Failure(failures))
    }

    private fun finishSuccessfully() {
        assert(isStartedImpl.get())
        assert(!isFinishedSuccessfully.getAndSet(true))
        configurationResult.complete(ProjectConfigurationResult.Success)
    }

    fun enqueue(stage: KotlinPluginLifecycle.Stage, action: KotlinPluginLifecycle.() -> Unit) {
        if (stage < this.stage) {
            throw KotlinPluginLifecycle.IllegalLifecycleException("Cannot enqueue Action for stage '$stage' in current stage '${this.stage}'")
        }

        /*
        Lifecycle finished: action shall not be enqueued, but just executed right away.
        This is desirable, so that .enqueue (and .launch) functions that are scheduled in execution phase
        will be executed right away (no suspend necessary or wanted)
        */
        if (isFinishedSuccessfully.get()) {
            return action()
        }

        /*
        Lifecycle finished, but some exceptions have been thrown.
        In this case, an enqueue for future Stages is not allowed, since those will not be executed anymore.
        Any enqueue in the current stage will be executed right away (no suspend necessary or wanted).
         */
        if (isFinishedWithFailures.get()) {
            return if (stage == this.stage) action()
            else Unit
        }

        enqueuedActions.getValue(stage).addLast(action)

        if (stage == KotlinPluginLifecycle.Stage.EvaluateBuildscript && isStartedImpl.get()) {
            loopIfNecessary()
        }
    }

    override fun launch(
        start: KotlinPluginLifecycle.CoroutineStart,
        block: suspend KotlinPluginLifecycle.() -> Unit,
    ) {
        val lifecycle = this

        val coroutine = block.createCoroutine(this, object : Continuation<Unit> {
            override val context: CoroutineContext = EmptyCoroutineContext +
                    KotlinPluginLifecycleCoroutineContextElement(lifecycle)

            override fun resumeWith(result: Result<Unit>) = result.getOrThrow()
        })

        when (start) {
            Default -> enqueue(stage) { coroutine.resume(Unit) }
            Undispatched -> coroutine.resume(Unit)
        }
    }

    override suspend fun await(stage: KotlinPluginLifecycle.Stage) {
        if (this.stage > stage) return
        suspendCoroutine<Unit> { continuation ->
            enqueue(stage) {
                continuation.resume(Unit)
            }
        }
    }

    override fun toString(): String = buildString {
        append("Kotlin Plugin Lifecycle: (${project.displayName})")
        if (!isStarted) append(" *not started*")
        else append(" stage '$stage'")
        if (isFinishedSuccessfully.get()) append(" *finished successfully*")
        if (isFinishedWithFailures.get()) append(" *finished with failures*")
    }
}

private class KotlinPluginLifecycleCoroutineContextElement(
    val lifecycle: KotlinPluginLifecycle,
) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<KotlinPluginLifecycleCoroutineContextElement>

    override val key: CoroutineContext.Key<KotlinPluginLifecycleCoroutineContextElement> = Key
}


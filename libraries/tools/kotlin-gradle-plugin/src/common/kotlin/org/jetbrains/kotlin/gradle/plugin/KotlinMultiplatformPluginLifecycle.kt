/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginLifecycle.IllegalLifecycleException
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginLifecycle.Stage
import org.jetbrains.kotlin.gradle.utils.getOrPut
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.*

internal val Project.kotlinMultiplatformPluginLifecycle: KotlinMultiplatformPluginLifecycle
    get() = extraProperties.getOrPut(KotlinMultiplatformPluginLifecycle::class.java.name) { KotlinMultiplatformPluginLifecycleImpl() }

internal fun Project.startKotlinMultiplatformPluginLifecycle() {
    (kotlinMultiplatformPluginLifecycle as KotlinMultiplatformPluginLifecycleImpl).start(this)
}

internal inline fun Project.enqueue(stage: Stage, crossinline action: Project.() -> Unit) {
    kotlinMultiplatformPluginLifecycle.enqueue(stage) { action() }
}

internal interface KotlinMultiplatformPluginLifecycle {
    enum class Stage {
        Configure,
        AfterEvaluate,

        BeforeFinaliseRefinesEdges,
        FinaliseRefinesEdges,
        AfterFinaliseRefinesEdges,

        BeforeFinaliseCompilations,
        FinaliseCompilations,
        AfterFinaliseCompilations,

        Finalised
    }

    val stage: Stage

    fun enqueue(stage: Stage, action: () -> Unit)

    fun launch(block: suspend KotlinMultiplatformPluginLifecycle.() -> Unit)

    suspend fun await(stage: Stage)

    class IllegalLifecycleException(message: String) : IllegalStateException(message)
}

private class KotlinMultiplatformPluginLifecycleImpl : KotlinMultiplatformPluginLifecycle {
    private val enqueuedStages: ArrayDeque<Stage> = ArrayDeque(Stage.values().toList())
    private val enqueuedActions: Map<Stage, ArrayDeque<() -> Unit>> = Stage.values().associateWith { ArrayDeque() }
    private var configureLoopRunning = AtomicBoolean(false)
    private var isStarted = AtomicBoolean(false)
    private var isFinished = AtomicBoolean(false)

    fun start(project: Project) {
        check(!isStarted.getAndSet(true)) {
            "${KotlinMultiplatformPluginLifecycle::class.java.name} already started"
        }

        check(!project.state.executed) {
            "${KotlinMultiplatformPluginLifecycle::class.java.name} cannot be started in ProjectState '${project.state}'"
        }

        project.whenEvaluated {
            executeStage(project, enqueuedStages.removeFirst())
        }
    }

    private fun executeStage(project: Project, stage: Stage) {
        this.stage = stage
        val queue = enqueuedActions.getValue(stage)
        do {
            val action = queue.removeFirstOrNull()
            action?.invoke()
        } while (action != null)
        val nextStage = enqueuedStages.removeFirstOrNull() ?: run {
            isFinished.set(true)
            return
        }

        project.afterEvaluate {
            executeStage(project, nextStage)
        }
    }

    private fun startConfigureLoopIfNecessary() {
        check(stage == Stage.Configure) { "Cannot start 'configure loop' on stage '$stage'" }
        if (configureLoopRunning.getAndSet(true)) return
        try {
            val queue = enqueuedActions.getValue(Stage.Configure)
            do {
                val action = queue.removeFirstOrNull()
                action?.invoke()
            } while (action != null)
        } finally {
            configureLoopRunning.set(false)
        }
    }

    override var stage: Stage = enqueuedStages.removeFirst()

    override fun enqueue(stage: Stage, action: () -> Unit) {
        if (stage < this.stage) {
            throw IllegalLifecycleException("Cannot enqueue Action for stage '${this.stage}' in current stage '${this.stage}'")
        }

        enqueuedActions.getValue(stage).addLast(action)

        if (stage == Stage.Configure) {
            startConfigureLoopIfNecessary()
        }
    }

    override fun launch(block: suspend KotlinMultiplatformPluginLifecycle.() -> Unit) {
        check(isStarted.get()) { "Cannot launch when ${KotlinMultiplatformPluginLifecycle::class.simpleName} is not started" }
        check(!isFinished.get()) { "Cannot launch when ${KotlinMultiplatformPluginLifecycle::class.simpleName} is already finished" }
        val coroutine = block.createCoroutine(this, object : Continuation<Unit> {
            override val context: CoroutineContext = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) = result.getOrThrow()
        })

        enqueue(stage) {
            coroutine.resume(Unit)
        }
    }

    override suspend fun await(stage: Stage) {
        if (this.stage >= stage) return
        suspendCoroutine<Unit> { continuation ->
            enqueue(stage) {
                continuation.resume(Unit)
            }
        }
    }
}

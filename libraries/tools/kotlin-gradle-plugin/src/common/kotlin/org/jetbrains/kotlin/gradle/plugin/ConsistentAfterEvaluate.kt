/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.completeWith
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.utils.getOrPut
import kotlin.coroutines.*

internal class SuspendableExecutor {
    private val supervisorJob = SupervisorJob()
    private val lateEvaluations = mutableListOf<suspend () -> Unit>()
    fun runNow(block: suspend () -> Unit) {
        val job = Job(supervisorJob)
        val continuation = Continuation<Unit>(job) { it.getOrThrow(); job.complete() }
        block.startCoroutine(continuation)
    }

    fun runLater(block: suspend () -> Unit) {
        lateEvaluations += block
    }

    fun runDeferredEvaluations() {
        try {
            while (lateEvaluations.isNotEmpty()) {
                val code = lateEvaluations.removeFirst()
                runNow { code() }
            }
            supervisorJob.complete()
        } catch (e: Throwable) {
            supervisorJob.completeExceptionally(e)
        }

        check(supervisorJob.children.all { it.isCompleted }) {
            "Following children are not completed: ${supervisorJob.children.filter { it.isActive }.toList()}"
        }
    }
}

internal interface SuspendableState<T> {
    fun getFinalValueOrThrow(): T
    suspend fun awaitFinalValue(): T
}

private class SuspendableStateImpl<T> : SuspendableState<T> {
    private val deferred = CompletableDeferred<T>()
    fun completeWith(result: Result<T>) {
        deferred.completeWith(result)
    }

    override fun getFinalValueOrThrow(): T {
        val exception = deferred.getCompletionExceptionOrNull()
        if (exception != null) throw exception
        return deferred.getCompleted()
    }

    override suspend fun awaitFinalValue(): T = deferred.await()
}

private fun <T> SuspendableExecutor.evaluate(code: suspend () -> T): SuspendableState<T> {
    val provider = SuspendableStateImpl<T>()
    runNow { provider.completeWith(runCatching { code() }) }
    return provider
}

internal interface SuspendableMutableState<T> : SuspendableState<T> {
    fun updateWith(code: suspend (currentValue: T) -> T)
}

private fun <T> SuspendableExecutor.mutableState(initialValue: T): SuspendableMutableState<T> {
    val property = SuspendableMutableStateImpl(initialValue)
    runLater { property.finalizeValue() }
    return property
}

private class SuspendableMutableStateImpl<T>(
    private val initialValue: T,
    private val state: SuspendableStateImpl<T> = SuspendableStateImpl()
): SuspendableMutableState<T> {
    private val mutations: MutableList<suspend (T) -> T> = mutableListOf()

    suspend fun finalizeValue() {
        val finalResult = runCatching {
            mutations.fold(initialValue) { currentValue, mutation -> mutation(currentValue) }
        }
        state.completeWith(finalResult)
    }

    override fun updateWith(code: suspend (currentValue: T) -> T) {
        mutations += code
    }

    override suspend fun awaitFinalValue(): T = state.awaitFinalValue()

    override fun getFinalValueOrThrow(): T = state.getFinalValueOrThrow()
}

private val Project.suspendableExecutor get() =
    extraProperties.getOrPut(SuspendableExecutor::class.java.name) {
        SuspendableExecutor()
    }

internal fun <T> Project.provider(code: suspend () -> T): Provider<T> {
    val suspendableState = suspendableExecutor.evaluate(code)
    return provider { suspendableState.getFinalValueOrThrow() }
}

internal fun <T> Project.suspendableProperty(initialValue: T): SuspendableMutableState<T> {
    val suspendableState = suspendableExecutor.mutableState(initialValue)
    return suspendableState
}

internal class SuspendableWrappers(
    private val executor: SuspendableExecutor
) {
    private val entities = mutableMapOf<Any, WrappedEntity<*>>()
    class WrappedEntity<T: Any> (
        val state: SuspendableMutableState<T>,
        val entity: T
    )

    fun <T: Any> getOrWrap(entity: T): WrappedEntity<T> {
        @Suppress("UNCHECKED_CAST")
        return entities.getOrPut(entity) {
            val state = executor.mutableState(entity)
            WrappedEntity(state, entity)
        } as WrappedEntity<T>
    }
}

fun main() {
    val evaluator = SuspendableExecutor()
    val p1 = evaluator.mutableState(emptyList<Int>())
    val p2 = evaluator.mutableState(emptyList<Int>())
    val p3 = evaluator.mutableState(emptyList<Int>())
    val p0 = evaluator.evaluate { p1.awaitFinalValue() }
    p1.updateWith { it + p2.awaitFinalValue() + 1 }
    p2.updateWith { it + p3.awaitFinalValue() + 2 }
    p3.updateWith { it + 3 }

    evaluator.runDeferredEvaluations()
    println(p0.getFinalValueOrThrow())
}
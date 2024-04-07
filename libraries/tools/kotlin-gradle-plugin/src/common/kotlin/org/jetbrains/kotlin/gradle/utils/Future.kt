/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.HasProject
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.CoroutineStart.Undispatched
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.IllegalLifecycleException
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle
import org.jetbrains.kotlin.tooling.core.HasMutableExtras
import java.io.Serializable
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.properties.ReadOnlyProperty

/**
 * See [KotlinPluginLifecycle]:
 * This [Future] represents a value that will be available in some 'future' time.
 *
 *
 * #### Simple use case example: Deferring the value of a property to a given [KotlinPluginLifecycle.Stage]:
 *
 * ```kotlin
 * val myFutureProperty: Future<Int> = project.future {
 *     await(FinaliseDsl) // <- suspends
 *     42
 * }
 * ```
 *
 * Futures can also be used to dynamically extend entities implementing [HasMutableExtras] and [HasProject]
 * #### Example Usage: Extending KotlinSourceSet with a property that relies on the final refines edges.
 *
 * ```kotlin
 * internal val KotlinSourceSet.dependsOnCommonMain: Future<Boolean> by lazyFuture("dependsOnCommonMain") {
 *    await(AfterFinaliseRefinesEdges)
 *    return dependsOn.contains { it.name == "commonMain") }
 * }
 * ```
 */
internal interface Future<out T> {
    suspend fun await(): T
    fun getOrThrow(): T
}

internal interface LenientFuture<T> : Future<T> {
    fun getOrNull(): T?
}

internal interface CompletableFuture<T> : Future<T> {
    val isCompleted: Boolean
    fun complete(value: T)
}

internal fun <T, R> Future<T>.map(transform: (T) -> R): Future<R> {
    return MappedFutureImpl(this, transform)
}

internal fun CompletableFuture<Unit>.complete() = complete(Unit)

/**
 * Extend a given [Receiver] with data produced by [block]:
 * This uses the [HasMutableExtras] infrastructure to store/share the produced future entity to the given [Receiver]
 * Note: The [block] will be lazily launched on first access to this extension!
 * @see extrasStoredProperty
 */
internal inline fun <Receiver, reified T> extrasStoredFuture(
    noinline block: suspend Receiver.() -> T,
): ReadOnlyProperty<Receiver, Future<T>> where Receiver : HasMutableExtras, Receiver : HasProject {
    return extrasStoredProperty {
        project.future { block() }
    }
}

internal fun <T> Project.future(
    start: KotlinPluginLifecycle.CoroutineStart = Undispatched,
    block: suspend Project.() -> T,
): Future<T> = kotlinPluginLifecycle.future(start) { block() }

internal val <T> Future<T>.lenient: LenientFuture<T> get() = LenientFutureImpl(this)

/**
 * Shortcut for
 * ```kotlin
 * lazy { future { block() } }
 * ```
 *
 * basically creating a future, which is launched lazily
 * (on first call to on any of the returned Future's method)
 */
internal fun <T> Project.lazyFuture(block: suspend Project.() -> T): Future<T> = LazyFutureImpl(lazy { future { block() } })

internal fun <T> KotlinPluginLifecycle.future(
    start: KotlinPluginLifecycle.CoroutineStart = Undispatched,
    block: suspend () -> T,
): Future<T> {
    return FutureImpl<T>(lifecycle = this).also { future ->
        launch(start) { future.completeWith(runCatching { block() }) }
    }
}

internal fun <T> CompletableFuture(): CompletableFuture<T> {
    return FutureImpl()
}

private class FutureImpl<T>(
    private val deferred: Completable<T> = Completable(),
    private val lifecycle: KotlinPluginLifecycle? = null,
) : CompletableFuture<T>, Serializable {
    fun completeWith(result: Result<T>) = deferred.completeWith(result)

    override val isCompleted: Boolean
        get() = deferred.isCompleted

    override fun complete(value: T) {
        deferred.complete(value)
    }

    override suspend fun await(): T {
        return deferred.await()
    }

    override fun getOrThrow(): T {
        return if (deferred.isCompleted) deferred.getCompleted() else throw IllegalLifecycleException(
            "Future was not completed yet" + if (lifecycle != null) " '$lifecycle'"
            else ""
        )
    }

    private fun writeReplace(): Any {
        return Surrogate(getOrThrow())
    }

    private class Surrogate<T>(private val value: T) : Serializable {
        private fun readResolve(): Any {
            return FutureImpl(Completable(value))
        }
    }
}

private class MappedFutureImpl<T, R>(
    private val future: Future<T>,
    private var transform: (T) -> R,
) : Future<R>, Serializable {

    private val value = Completable<R>()

    override suspend fun await(): R {
        if (value.isCompleted) return value.getCompleted()
        // await can happen concurrently, but only one of them will go to the critical block
        // and actually perform transformation.
        // others will be early-returned
        val valueToMap = future.await()
        synchronized(value) {
            if (value.isCompleted) return@synchronized
            value.complete(transform(valueToMap))
            transform = { throw IllegalStateException("Unexpected 'transform' in future") }
        }
        return value.getCompleted()
    }

    override fun getOrThrow(): R = synchronized(value) {
        if (value.isCompleted) return value.getCompleted()
        value.complete(transform(future.getOrThrow()))
        transform = { throw IllegalStateException("Unexpected 'transform' in future") }
        value.getCompleted()
    }

    private fun writeReplace(): Any {
        return Surrogate(getOrThrow())
    }

    private class Surrogate<T>(private val value: T) : Serializable {
        private fun readResolve(): Any {
            return FutureImpl(Completable(value))
        }
    }
}

private class LenientFutureImpl<T>(
    private val future: Future<T>,
) : LenientFuture<T>, Serializable {
    override suspend fun await(): T {
        return future.await()
    }

    override fun getOrThrow(): T {
        return future.getOrThrow()
    }

    override fun getOrNull(): T? {
        return try {
            future.getOrThrow()
        } catch (t: IllegalLifecycleException) {
            return null
        }
    }

    private fun writeReplace(): Any {
        return Surrogate(getOrNull())
    }

    private class Surrogate<T>(private val value: T) : Serializable {
        private fun readResolve(): Any {
            return LenientFutureImpl(FutureImpl(Completable(value)))
        }
    }
}

private class LazyFutureImpl<T>(private val future: Lazy<Future<T>>) : Future<T>, Serializable {
    override suspend fun await(): T {
        return future.value.await()
    }

    override fun getOrThrow(): T {
        return future.value.getOrThrow()
    }

    private fun writeReplace(): Any {
        return Surrogate(getOrThrow())
    }

    private class Surrogate<T>(private val value: T) : Serializable {
        private fun readResolve(): Any {
            return FutureImpl(Completable(value))
        }
    }
}

/**
 * Simple, with primitive synchronization, replacement for kotlinx.coroutines.CompletableDeferred.
 */
private class Completable<T>(
    private var value: Result<T>? = null,
) {
    constructor(value: T) : this(Result.success(value))

    private val lock = ReentrantReadWriteLock()

    val isCompleted: Boolean get() = lock.read { value != null }

    private val waitingContinuations = mutableListOf<Continuation<Result<T>>>()

    fun completeWith(result: Result<T>) {
        val continuations = lock.write {
            check(value == null) { "Already completed with $value" }
            value = result

            /* Capture and clear current waiting continuations */
            waitingContinuations.toList().also { waitingContinuations.clear() }
        }

        /** it is safe to process continuations outside write lock
         * because after write block all [await] calls will be shortcut due to [value] presence
         * thus no more [waitingContinuations] adding. */
        continuations.forEach { continuation ->
            continuation.resume(result)
        }
    }

    fun complete(value: T) {
        completeWith(Result.success(value))
    }

    fun getCompleted(): T = lock.read {
        val value = this.value ?: throw IllegalStateException("Not completed yet")
        value.getOrThrow()
    }

    suspend fun await(): T {
        val readLock = lock.readLock()
        readLock.lock()
        val value = this.value
        if (value != null) {
            return value.getOrThrow().also { readLock.unlock() }
        }

        return suspendCoroutine<Result<T>> { continuation ->
            waitingContinuations.add(continuation)
            /** As soon as we add to waitlist we can release the lock
             * so during [completeWith] continuation will be completed. */
            readLock.unlock()
        }.getOrThrow()
    }
}

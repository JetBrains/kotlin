/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.*
import org.jetbrains.kotlin.gradle.utils.getOrPut
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayDeque
import kotlin.collections.set
import kotlin.coroutines.*
import kotlin.reflect.KProperty

/*
Util functions
 */

internal fun Project.launch(block: suspend KotlinPluginLifecycle.() -> Unit) {
    kotlinPluginLifecycle.launch(block)
}

internal fun Project.launchInStage(stage: Stage, block: suspend KotlinPluginLifecycle.() -> Unit) {
    launch {
        await(stage)
        block()
    }
}

internal fun Project.launchInRequiredStage(stage: Stage, block: suspend KotlinPluginLifecycle.() -> Unit) {
    launchInStage(stage) {
        requiredStage(stage) {
            block()
        }
    }
}

internal val Project.kotlinPluginLifecycle: KotlinPluginLifecycle
    get() = extraProperties.getOrPut(KotlinPluginLifecycle::class.java.name) {
        KotlinPluginLifecycleImpl(project)
    }

internal fun Project.startKotlinPluginLifecycle() {
    (kotlinPluginLifecycle as KotlinPluginLifecycleImpl).start()
}

internal suspend fun currentKotlinPluginLifecycle(): KotlinPluginLifecycle {
    return currentCoroutineContext()[KotlinMultiplatformPluginLifecycleCoroutineContextElement]?.lifecycle
        ?: error("Missing $KotlinMultiplatformPluginLifecycleCoroutineContextElement in currentCoroutineContext")
}

internal suspend fun await(stage: Stage) {
    currentKotlinPluginLifecycle().await(stage)
}

internal inline fun <reified T : Any> Project.newKotlinPluginLifecycleAwareProperty(
    finaliseIn: Stage = Stage.FinaliseDsl, initialValue: T? = null
): LifecycleAwareProperty<T> {
    return kotlinPluginLifecycle.newLifecycleAwareProperty(T::class.java, finaliseIn, initialValue)
}

internal suspend fun <T : Any> Property<T>.findKotlinPluginLifecycleAwareProperty(): LifecycleAwareProperty<T>? {
    return (currentKotlinPluginLifecycle() as KotlinPluginLifecycleImpl).findLifecycleAwareProperty(this)
}

internal suspend fun <T : Any> Property<T>.awaitFinalValue(): T? {
    val lifecycleAwareProperty = findKotlinPluginLifecycleAwareProperty()
        ?: throw IllegalArgumentException("Property is not lifecycle aware")
    return lifecycleAwareProperty.awaitFinalValue()
}

internal suspend fun Property<*>.isKotlinPluginLifecycleAware(): Boolean {
    return findKotlinPluginLifecycleAwareProperty() != null
}

internal suspend fun <T> requiredStage(stage: Stage, block: suspend () -> T): T {
    return withRestrictedStages(hashSetOf(stage), block)
}

internal suspend fun <T> requireCurrentStage(block: suspend () -> T): T {
    return requiredStage(currentKotlinPluginLifecycle().stage, block)
}

internal suspend fun <T> withRestrictedStages(allowed: Set<Stage>, block: suspend () -> T): T {
    return withContext(RestrictedLifecycleStages(currentKotlinPluginLifecycle(), allowed)) {
        block()
    }
}

/*
Definition of the Lifecycle and its stages
 */

internal interface KotlinPluginLifecycle {
    enum class Stage {
        Configure,
        AfterEvaluate,

        BeforeFinaliseDsl,
        FinaliseDsl,
        AfterFinaliseDsl,

        BeforeFinaliseRefinesEdges,
        FinaliseRefinesEdges,
        AfterFinaliseRefinesEdges,

        BeforeFinaliseCompilations,
        FinaliseCompilations,
        AfterFinaliseCompilations,

        Finalised;

        val previousOrFirst: Stage get() = previousOrNull ?: values.first()
        val previousOrNull: Stage? get() = values.getOrNull(ordinal - 1)
        val nextOrNull: Stage? get() = values.getOrNull(ordinal + 1)
        val nextOrLast: Stage get() = nextOrNull ?: values.last()
        val nextOrThrow: Stage get() = values[ordinal + 1]

        operator fun rangeTo(other: Stage): Set<Stage> {
            return values.subList(this.ordinal, other.ordinal + 1).toSet()
        }

        companion object {
            val values = values().toList()
            fun upTo(stage: Stage): Set<Stage> = values.first()..stage
            fun until(stage: Stage): Set<Stage> {
                return upTo(stage.previousOrNull ?: return emptySet())
            }
        }
    }

    val project: Project

    val stage: Stage

    fun enqueue(stage: Stage, action: KotlinPluginLifecycle.() -> Unit)

    fun launch(block: suspend KotlinPluginLifecycle.() -> Unit)

    suspend fun await(stage: Stage)

    fun <T : Any> newLifecycleAwareProperty(
        type: Class<T>, finaliseIn: Stage, initialValue: T?
    ): LifecycleAwareProperty<T>

    class IllegalLifecycleException(message: String) : IllegalStateException(message)

    interface LifecycleAwareProperty<T : Any> {
        val finaliseIn: Stage
        val property: Property<T>
        suspend fun awaitFinalValue(): T?
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Property<T> = this.property
    }
}


/*
Implementation
 */

private class KotlinPluginLifecycleImpl(override val project: Project) : KotlinPluginLifecycle {
    private val enqueuedActions: Map<Stage, ArrayDeque<KotlinPluginLifecycle.() -> Unit>> =
        Stage.values().associateWith { ArrayDeque() }

    private var loopRunning = AtomicBoolean(false)
    private var isStarted = AtomicBoolean(false)
    private var isFinished = AtomicBoolean(false)

    private val properties = WeakHashMap<Property<*>, WeakReference<LifecycleAwareProperty<*>>>()

    fun start() {
        check(!isStarted.getAndSet(true)) {
            "${KotlinPluginLifecycle::class.java.name} already started"
        }

        check(!project.state.executed) {
            "${KotlinPluginLifecycle::class.java.name} cannot be started in ProjectState '${project.state}'"
        }

        project.whenEvaluated {
            assert(enqueuedActions.getValue(stage).isEmpty()) { "Expected empty queue from '$stage'" }
            executeStage(project, stage.nextOrThrow)
        }
    }

    private fun executeStage(project: Project, stage: Stage) {
        this.stage = stage

        loopIfNecessary()

        val nextStage = stage.nextOrNull ?: run {
            isFinished.set(true)
            return
        }

        project.afterEvaluate {
            executeStage(project, nextStage)
        }
    }

    private fun loopIfNecessary() {
        if (loopRunning.getAndSet(true)) return
        try {
            val queue = enqueuedActions.getValue(stage)
            do {
                val action = queue.removeFirstOrNull()
                action?.invoke(this)
            } while (action != null)
        } finally {
            loopRunning.set(false)
        }
    }

    override var stage: Stage = Stage.values.first()

    override fun enqueue(stage: Stage, action: KotlinPluginLifecycle.() -> Unit) {
        if (stage < this.stage) {
            throw IllegalLifecycleException("Cannot enqueue Action for stage '${this.stage}' in current stage '${this.stage}'")
        }

        enqueuedActions.getValue(stage).addLast(action)

        if (stage == Stage.Configure) {
            loopIfNecessary()
        }
    }

    override fun launch(block: suspend KotlinPluginLifecycle.() -> Unit) {
        val lifecycle = this
        check(isStarted.get()) { "Cannot launch when ${KotlinPluginLifecycle::class.simpleName} is not started" }
        check(!isFinished.get()) { "Cannot launch when ${KotlinPluginLifecycle::class.simpleName} is already finished" }

        val coroutine = block.createCoroutine(this, object : Continuation<Unit> {
            override val context: CoroutineContext = EmptyCoroutineContext +
                    KotlinMultiplatformPluginLifecycleCoroutineContextElement(lifecycle)

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

    override fun <T : Any> newLifecycleAwareProperty(type: Class<T>, finaliseIn: Stage, initialValue: T?): LifecycleAwareProperty<T> {
        val property = project.objects.property(type)
        if (initialValue != null) property.set(initialValue)
        enqueue(finaliseIn) { property.finalizeValue() }
        val lifecycleAwareProperty = LifecycleAwarePropertyImpl(finaliseIn, property)
        properties[property] = WeakReference(lifecycleAwareProperty)
        return lifecycleAwareProperty
    }

    fun <T : Any> findLifecycleAwareProperty(property: Property<T>): LifecycleAwareProperty<T>? {
        @Suppress("UNCHECKED_CAST")
        return properties[property]?.get() as? LifecycleAwareProperty<T>
    }

    private class LifecycleAwarePropertyImpl<T : Any>(
        override val finaliseIn: Stage,
        override val property: Property<T>
    ) : LifecycleAwareProperty<T> {
        override suspend fun awaitFinalValue(): T? {
            await(finaliseIn)
            return property.orNull
        }
    }
}

private class KotlinMultiplatformPluginLifecycleCoroutineContextElement(
    val lifecycle: KotlinPluginLifecycle
) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<KotlinMultiplatformPluginLifecycleCoroutineContextElement>

    override val key: CoroutineContext.Key<KotlinMultiplatformPluginLifecycleCoroutineContextElement> = Key
}

private class RestrictedLifecycleStages(
    private val lifecycle: KotlinPluginLifecycle,
    private val allowedStages: Set<Stage>,
) : CoroutineContext.Element, ContinuationInterceptor {
    @OptIn(ExperimentalStdlibApi::class)
    companion object Key : AbstractCoroutineContextKey<ContinuationInterceptor, RestrictedLifecycleStages>(
        ContinuationInterceptor, { it as? RestrictedLifecycleStages }
    )

    override val key: CoroutineContext.Key<*> = Key

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = object : Continuation<T> {
        override val context: CoroutineContext
            get() = continuation.context

        override fun resumeWith(result: Result<T>) = when {
            result.isFailure -> continuation.resumeWith(result)
            lifecycle.stage !in allowedStages -> continuation.resumeWithException(
                IllegalLifecycleException(
                    "Required stage in '$allowedStages', but lifecycle switched to '${lifecycle.stage}'"
                )
            )
            else -> continuation.resumeWith(result)
        }
    }

    init {
        if (lifecycle.stage !in allowedStages) {
            throw IllegalLifecycleException("Required stage in '${allowedStages}' but lifecycle is currently in '${lifecycle.stage}'")
        }
    }
}

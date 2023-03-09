/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginLifecycle.*
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

internal fun Project.launch(block: suspend KotlinMultiplatformPluginLifecycle.() -> Unit) {
    kotlinMultiplatformPluginLifecycle.launch(block)
}

internal fun Project.launchInStage(stage: Stage, block: suspend KotlinMultiplatformPluginLifecycle.() -> Unit) {
    launch {
        await(stage)
        block()
    }
}

internal fun Project.launchInRequiredStage(stage: Stage, block: suspend KotlinMultiplatformPluginLifecycle.() -> Unit) {
    launchInStage(stage) {
        requiredStage(stage) {
            block()
        }
    }
}

internal val Project.kotlinMultiplatformPluginLifecycle: KotlinMultiplatformPluginLifecycle
    get() = extraProperties.getOrPut(KotlinMultiplatformPluginLifecycle::class.java.name) {
        KotlinMultiplatformPluginLifecycleImpl(project)
    }

internal fun Project.startKotlinMultiplatformPluginLifecycle() {
    (kotlinMultiplatformPluginLifecycle as KotlinMultiplatformPluginLifecycleImpl).start()
}

internal suspend fun currentMultiplatformPluginLifecycle(): KotlinMultiplatformPluginLifecycle {
    return currentCoroutineContext()[KotlinMultiplatformPluginLifecycleCoroutineContextElement]?.lifecycle
        ?: error("Missing $KotlinMultiplatformPluginLifecycleCoroutineContextElement in currentCoroutineContext")
}

internal suspend fun await(stage: Stage) {
    currentMultiplatformPluginLifecycle().await(stage)
}

internal inline fun <reified T : Any> Project.newLifecycleAwareProperty(
    finaliseIn: Stage = Stage.FinaliseDsl, initialValue: T? = null
): LifecycleAwareProperty<T> {
    return kotlinMultiplatformPluginLifecycle.newLifecycleAwareProperty(T::class.java, finaliseIn, initialValue)
}

internal suspend fun <T : Any> Property<T>.findLifecycleAwareProperty(): LifecycleAwareProperty<T>? {
    return (currentMultiplatformPluginLifecycle() as KotlinMultiplatformPluginLifecycleImpl).findLifecycleAwareProperty(this)
}

internal suspend fun <T : Any> Property<T>.awaitFinalValue(): T? {
    val lifecycleAwareProperty = findLifecycleAwareProperty() ?: throw IllegalArgumentException("Property is not lifecycle aware")
    return lifecycleAwareProperty.awaitFinalValue()
}

internal suspend fun Property<*>.isLifecycleAware(): Boolean {
    return findLifecycleAwareProperty() != null
}

internal suspend fun <T> requiredStage(stage: Stage, block: suspend () -> T): T {
    return withRestrictedStages(hashSetOf(stage), block)
}

internal suspend fun <T> requireCurrentStage(block: suspend () -> T): T {
    return requiredStage(currentMultiplatformPluginLifecycle().stage, block)
}

internal suspend fun <T> withRestrictedStages(allowed: Set<Stage>, block: suspend () -> T): T {
    return withContext(RestrictedLifecycleStages(currentMultiplatformPluginLifecycle(), allowed)) {
        block()
    }
}

/*
Definition of the Lifecycle and its stages
 */

internal interface KotlinMultiplatformPluginLifecycle {
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

        operator fun rangeTo(other: Stage): Set<Stage> {
            return values.subList(this.ordinal, other.ordinal + 1).toSet()
        }

        companion object {
            val values = values().toList()
            fun upTo(stage: Stage): Set<Stage> = values.first()..stage
        }
    }

    val project: Project

    val stage: Stage

    fun enqueue(stage: Stage, action: KotlinMultiplatformPluginLifecycle.() -> Unit)

    fun launch(block: suspend KotlinMultiplatformPluginLifecycle.() -> Unit)

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

private class KotlinMultiplatformPluginLifecycleImpl(override val project: Project) : KotlinMultiplatformPluginLifecycle {
    private val enqueuedStages: ArrayDeque<Stage> = ArrayDeque(Stage.values)
    private val enqueuedActions: Map<Stage, ArrayDeque<KotlinMultiplatformPluginLifecycle.() -> Unit>> =
        Stage.values().associateWith { ArrayDeque() }

    private var configureLoopRunning = AtomicBoolean(false)
    private var isStarted = AtomicBoolean(false)
    private var isFinished = AtomicBoolean(false)

    private val properties = WeakHashMap<Property<*>, WeakReference<LifecycleAwareProperty<*>>>()

    fun start() {
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
            action?.invoke(this)
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
                action?.invoke(this)
            } while (action != null)
        } finally {
            configureLoopRunning.set(false)
        }
    }

    override var stage: Stage = enqueuedStages.removeFirst()

    override fun enqueue(stage: Stage, action: KotlinMultiplatformPluginLifecycle.() -> Unit) {
        if (stage < this.stage) {
            throw IllegalLifecycleException("Cannot enqueue Action for stage '${this.stage}' in current stage '${this.stage}'")
        }

        enqueuedActions.getValue(stage).addLast(action)

        if (stage == Stage.Configure) {
            startConfigureLoopIfNecessary()
        }
    }

    override fun launch(block: suspend KotlinMultiplatformPluginLifecycle.() -> Unit) {
        val lifecycle = this
        check(isStarted.get()) { "Cannot launch when ${KotlinMultiplatformPluginLifecycle::class.simpleName} is not started" }
        check(!isFinished.get()) { "Cannot launch when ${KotlinMultiplatformPluginLifecycle::class.simpleName} is already finished" }

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
    val lifecycle: KotlinMultiplatformPluginLifecycle
) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<KotlinMultiplatformPluginLifecycleCoroutineContextElement>

    override val key: CoroutineContext.Key<KotlinMultiplatformPluginLifecycleCoroutineContextElement> = Key
}

private class RestrictedLifecycleStages(
    private val lifecycle: KotlinMultiplatformPluginLifecycle,
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

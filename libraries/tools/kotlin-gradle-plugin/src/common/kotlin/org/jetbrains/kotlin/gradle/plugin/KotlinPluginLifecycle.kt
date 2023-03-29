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

/**
 * Launches the given [block] as coroutine inside the Kotlin Gradle Plugin which allows to suspend execution of the code.
 * Intended use cases for suspensions are:
 *
 * #### Waiting for a given [KotlinPluginLifecycle.Stage]
 *
 * ```kotlin
 * project.launch {
 *     // code
 *     await(Stage.AfterEvaluate) // <- suspends
 *     assertEquals(Stage.AfterEvaluate, stage)
 *
 *     await(Stage.FinaliseDsl) // suspends
 *     assertEquals(Stage.FinaliseDsl, stage)
 *     // code
 * }
 * ```
 *
 * #### Waiting for some Gradle property to return a final value
 *
 * ```kotlin
 * project.launch {
 *     val value = myProperty.awaitFinalValue() // <- suspends until final value is available!
 * }
 * ```
 *
 * #### Waiting for other suspending code in the Kotlin Gradle Plugin
 *
 * ```kotlin
 * project.launch {
 *    // code
 *    callMyOtherSuspendingFunction() // <- suspends
 * }
 * ```
 *
 * When launching a coroutine, the execution start of the [block] is not guaranteed.
 * It can be executed right away, effectively executing the [block] before this launch function returns
 * However, when called inside an already existing coroutine (or once Gradle has started executing afterEvaluate listeners),
 * then this block executed after this launch function returns and put at the end of the execution queue
 *
 * If the lifecycle already finished and Gradle moved to its execution phase, then the block will be invoked right away.
 */
internal fun Project.launch(block: suspend KotlinPluginLifecycle.() -> Unit) {
    kotlinPluginLifecycle.launch(block)
}

/**
 * See [launch] and [launchInRequiredStage]
 *
 * This is a shortcut to [launch] and immediately awaiting the specified [stage]:
 * @param block Is guaranteed to be executed *not before* [stage]. However, when this function is called in a higher stage then specified
 * the [block] will still be launched.
 */
internal fun Project.launchInStage(stage: Stage, block: suspend KotlinPluginLifecycle.() -> Unit) {
    launch {
        await(stage)
        block()
    }
}

/**
 * See also [launch]
 *
 * Launches the given block in the specified lifecycle stage [stage].
 * It is guaranteed that [block] is only executed in the specified [stage]. Leaving the stage is forbidden.
 *
 * @throws IllegalLifecycleException if the [stage] was already executed or [block] tries to exit the required [stage]
 *
 * ```kotlin
 * project.launchInRequiredStage(Stage.BeforeFinaliseDsl) {
 *     assertEquals(Stage.BeforeFinaliseDsl, stage) // guaranteed!
 *     assertFails { await(Stage.FinaliseDsl) } // <- forbidden, as it tried to leave the required stage!
 * }
 * ```
 */
internal fun Project.launchInRequiredStage(stage: Stage, block: suspend KotlinPluginLifecycle.() -> Unit) {
    launchInStage(stage) {
        requiredStage(stage) {
            block()
        }
    }
}

/**
 * Universal way of retrieving the current lifecycle
 * Also: See [currentKotlinPluginLifecycle]
 */
internal val Project.kotlinPluginLifecycle: KotlinPluginLifecycle
    get() = extraProperties.getOrPut(KotlinPluginLifecycle::class.java.name) {
        KotlinPluginLifecycleImpl(project)
    }

/**
 * Will start the lifecycle, this shall be called before the [kotlinPluginLifecycle] is effectively used
 */
internal fun Project.startKotlinPluginLifecycle() {
    (kotlinPluginLifecycle as KotlinPluginLifecycleImpl).start()
}

/**
 * Similar to [currentCoroutineContext]: Returns the current [KotlinPluginLifecycle] instance used to launch
 * the currently running coroutine. Throws if this coroutine was not started using a [KotlinPluginLifecycle]
 */
internal suspend fun currentKotlinPluginLifecycle(): KotlinPluginLifecycle {
    return currentCoroutineContext()[KotlinPluginLifecycleCoroutineContextElement]?.lifecycle
        ?: error("Missing $KotlinPluginLifecycleCoroutineContextElement in currentCoroutineContext")
}

/**
 * Suspends execution until we *at least* reached the specified [this@await]
 * This will return right away if the specified [this@await] was already executed or we are currently executing the [this@await]
 */
internal suspend fun Stage.await() {
    currentKotlinPluginLifecycle().await(this)
}

/**
 * See [LifecycleAwareProperty]
 * Will create a new [LifecycleAwareProperty] which is going to finalise its value in stage [finaliseIn]
 * and the initialValue [initialValue]
 *
 * ## Sample
 * ```kotlin
 * val myProperty by project.newKotlinPluginLifecycleAwareProperty<String>()
 * myProperty.set("hello")
 * //...
 * project.launch {
 *     val myFinalValue = myProperty.awaitFinalValue() // <- suspends until final value is known!
 * }
 * ```
 */
internal inline fun <reified T : Any> Project.newKotlinPluginLifecycleAwareProperty(
    finaliseIn: Stage = Stage.FinaliseDsl, initialValue: T? = null
): LifecycleAwareProperty<T> {
    return kotlinPluginLifecycle.newLifecycleAwareProperty(T::class.java, finaliseIn, initialValue)
}

/**
 * Will return the [LifecycleAwareProperty] instance if the given receiver was created by [newKotlinPluginLifecycleAwareProperty]
 */
internal suspend fun <T : Any> Property<T>.findKotlinPluginLifecycleAwareProperty(): LifecycleAwareProperty<T>? {
    return (currentKotlinPluginLifecycle() as KotlinPluginLifecycleImpl).findLifecycleAwareProperty(this)
}

/**
 * Will suspend until the property finalises its value and therefore a final value can returned.
 * Note: This only works on properties that are [isKotlinPluginLifecycleAware]
 * (e.g. by being created using [newKotlinPluginLifecycleAwareProperty]).
 *
 * If a property was not created using 'newKotlinPluginLifecycleAwareProperty' then the execution
 * will suspend until 'FinaliseDsl' and calls [Property.finalizeValue] before returnign the actual value
 */
internal suspend fun <T : Any> Property<T>.awaitFinalValue(): T? {
    val lifecycleAwareProperty = findKotlinPluginLifecycleAwareProperty()
    if (lifecycleAwareProperty != null) {
        return lifecycleAwareProperty.awaitFinalValue()
    }

    Stage.FinaliseDsl.await()
    finalizeValue()
    return orNull
}

/**
 * @return true if this property has an associated [LifecycleAwareProperty]
 */
internal suspend fun Property<*>.isKotlinPluginLifecycleAware(): Boolean {
    return findKotlinPluginLifecycleAwareProperty() != null
}

/**
 * See also [withRestrictedStages]
 *
 * Will ensure that the given [block] can only execute in the given [stage]
 */
internal suspend fun <T> requiredStage(stage: Stage, block: suspend () -> T): T {
    return withRestrictedStages(hashSetOf(stage), block)
}

/**
 * See also [withRestrictedStages]
 *
 * Will ensure that the given [block] cannot leave the current stage
 * e.g.
 *
 * ```kotlin
 * project.launch {
 *    requireCurrentStage {
 *        await(stage.nextOrThrow) // <- fails! We are not allowed to switch stages!
 *    }
 * }
 * ```
 */
internal suspend fun <T> requireCurrentStage(block: suspend () -> T): T {
    return requiredStage(currentKotlinPluginLifecycle().stage, block)
}

/**
 * Will ensure that the given [block] cannot leave the specified allowed stages [allowed]
 * e.g.
 *
 * ```kotlin
 * project.launchInStage(Stage.BeforeFinaliseDsl) {
 *     withRestrictedStages(Stage.upTo(Stage.FinaliseDsl)) {
 *        await(Stage.FinaliseDsl) // <- OK, since still in allowed stages
 *        await(Stage.AfterFinaliseDsl) // <- fails, since not in allowed stages!
 *     }
 * }
 * ```
 */
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
        /**
         * Configure Phase of Gradle: No .afterEvaluate {} listeners have been called yet,
         * the buildscript is still evaluated!
         */
        EvaluateBuildscript,
        AfterEvaluateBuildscript,

        /**
         * Last changes are allowed to be done to the DSL.
         * E.g. Gradle properties shall call their [Property.finalizeValue] functions to
         * disallow further changes
         */
        FinaliseDsl,
        AfterFinaliseDsl,


        /**
         * All refines edges ([KotlinSourceSet.dependsOn]) have to be finalised here.
         * Adding edges after this stage is forbidden and will throw an exception!
         */
        FinaliseRefinesEdges,
        AfterFinaliseRefinesEdges,


        /**
         * [KotlinCompilation] instances have to finalised: Creating compilations after this stage is forbidden.
         * Values and configuration of compilations also shall be finalised already
         */
        FinaliseCompilations,
        AfterFinaliseCompilations,

        /**
         * Done; Configuration Phase passed. All tasks are configured and execution can be scheduled
         */
        ReadyForExecution;

        val previousOrFirst: Stage get() = previousOrNull ?: values.first()
        val previousOrNull: Stage? get() = values.getOrNull(ordinal - 1)
        val previousOrThrow: Stage
            get() = previousOrNull ?: throw IllegalArgumentException("'$this' does not have a next ${Stage::class.simpleName}")
        val nextOrNull: Stage? get() = values.getOrNull(ordinal + 1)
        val nextOrLast: Stage get() = nextOrNull ?: values.last()
        val nextOrThrow: Stage
            get() = nextOrNull ?: throw IllegalArgumentException("'$this' does not have a next ${Stage::class.simpleName}")

        operator fun rangeTo(other: Stage): Set<Stage> {
            if (this.ordinal > other.ordinal) return emptySet()
            return values.subList(this.ordinal, other.ordinal + 1).toSet()
        }

        companion object {
            val values = values().toList()
            val first = values.first()
            val last = values.last()
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

    /**
     * Wrapper around Gradle's [Property] that is aware of the [KotlinPluginLifecycle] and ensures that
     * the given [property] is finalised in [stage] (also calling [Property.finalizeValue]).
     *
     * A property finalised in a given [stage] will allow to safely get the final value using the
     * [awaitFinalValue] function, suspending the execution until the value is indeed finalised.
     *
     * See [Project.newKotlinPluginLifecycleAwareProperty] to create a new instance
     */
    interface LifecycleAwareProperty<T : Any> {
        val finaliseIn: Stage
        val property: Property<T>

        /**
         * See [LifecycleAwareProperty]
         */
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

    private val loopRunning = AtomicBoolean(false)
    private val isStarted = AtomicBoolean(false)
    private val isFinished = AtomicBoolean(false)

    private val properties = WeakHashMap<Property<*>, WeakReference<LifecycleAwareProperty<*>>>()

    fun start() {
        check(!isStarted.getAndSet(true)) {
            "${KotlinPluginLifecycle::class.java.name} already started"
        }

        check(!project.state.executed) {
            "${KotlinPluginLifecycle::class.java.name} cannot be started in ProjectState '${project.state}'"
        }

        loopIfNecessary()

        project.whenEvaluated {
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

        loopIfNecessary()

        stage = stage.nextOrNull ?: run {
            isFinished.set(true)
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

        /*
        Lifecycle finished: action shall not be enqueued, but just executed right away.
        This is desirable, so that .enqueue (and .launch) functions that are scheduled in execution phase
        will be executed right away (no suspend necessary or wanted)
        */
        if (isFinished.get()) {
            action()
            return
        }

        enqueuedActions.getValue(stage).addLast(action)

        if (stage == Stage.EvaluateBuildscript && isStarted.get()) {
            loopIfNecessary()
        }
    }

    override fun launch(block: suspend KotlinPluginLifecycle.() -> Unit) {
        val lifecycle = this

        val coroutine = block.createCoroutine(this, object : Continuation<Unit> {
            override val context: CoroutineContext = EmptyCoroutineContext +
                    KotlinPluginLifecycleCoroutineContextElement(lifecycle)

            override fun resumeWith(result: Result<Unit>) = result.getOrThrow()
        })

        enqueue(stage) {
            coroutine.resume(Unit)
        }
    }

    override suspend fun await(stage: Stage) {
        if (this.stage > stage) return
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
            finaliseIn.await()
            return property.orNull
        }
    }
}

private class KotlinPluginLifecycleCoroutineContextElement(
    val lifecycle: KotlinPluginLifecycle
) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<KotlinPluginLifecycleCoroutineContextElement>

    override val key: CoroutineContext.Key<KotlinPluginLifecycleCoroutineContextElement> = Key
}

private class RestrictedLifecycleStages(
    private val lifecycle: KotlinPluginLifecycle,
    private val allowedStages: Set<Stage>,
) : CoroutineContext.Element, ContinuationInterceptor {

    override val key: CoroutineContext.Key<*> = ContinuationInterceptor

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

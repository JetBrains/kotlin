/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.*
import org.jetbrains.kotlin.gradle.utils.CompletableFuture
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.failures
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
 * Future that will be completed once the project is considered 'Configured'
 * ### Happy Path
 * If the project configuration is successful (no exceptions thrown), then this Future will complete
 * **after** [KotlinPluginLifecycle.Stage.ReadyForExecution] was fully executed. All coroutines within the regular lifecycle .
 * In this case the value of this future will be [ProjectConfigurationResult.Success]
 *
 * ### Unhappy Path (Project configuration failed via exception)
 * If the project configuration is unsuccessful (exception thrown) then this future will complete with
 * [ProjectConfigurationResult.Failure], carrying the thrown exceptions.
 *
 * E.g. the following code:
 * ```kotlin
 * project.launchInStage(Stage.FinaliseCompilations) {
 *     throw Exception("My Error")
 * }
 * ```
 *
 * will lead to:
 * ```kotlin
 * project.launch {
 *     val result = project.configurationResult.await()
 *     val result as Failure
 *     val exception = result.failures.first()
 *     println(exception.message) // 'My Error'
 *     println(stage) // 'Stage.FinaliseCompilations'
 * }
 * ```
 *
 * #### Failure case | Launching coroutines | Future.getOrThrow
 * Even in case of failure it is still okay to further launch a new coroutine
 * ```kotlin
 * project.launch {
 *    val result = project.configurationResult.await() as Failure
 *    val anotherJob = project.launch { ... } // <- executed right away
 *    val someFutureEvaluation = project.someFuture.getOrThrow() // <- will return value if all 'requirements' have been met.
 * }
 * ```
 *
 * Note: [Future.getOrThrow] will throw if e.g. the lifecycle fails in a very early stage, but the Future requires
 * some later data to be available. In this case, the Future still will only return 'sane' data.
 */
internal val Project.configurationResult: Future<ProjectConfigurationResult>
    get() = configurationResultImpl


private val Project.configurationResultImpl: CompletableFuture<ProjectConfigurationResult>
    get() = extraProperties.getOrPut("org.jetbrains.kotlin.gradle.plugin.configurationResult") { CompletableFuture() }


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
    return coroutineContext[KotlinPluginLifecycleCoroutineContextElement]?.lifecycle
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
 * See [newProperty]
 */
internal inline fun <reified T : Any> Project.newKotlinPluginLifecycleAwareProperty(
    finaliseIn: Stage = Stage.FinaliseDsl, initialValue: T? = null,
): LifecycleAwareProperty<T> {
    return kotlinPluginLifecycle.newProperty(T::class.java, finaliseIn, initialValue)
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
internal inline fun <reified T : Any> KotlinPluginLifecycle.newProperty(
    finaliseIn: Stage = Stage.FinaliseDsl, initialValue: T? = null,
): LifecycleAwareProperty<T> {
    return newProperty(T::class.java, finaliseIn, initialValue)
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
    val newCoroutineContext = coroutineContext + RestrictedLifecycleStages(currentKotlinPluginLifecycle(), allowed)
    return suspendCoroutine { continuation ->
        val newContinuation = object : Continuation<T> {
            override val context: CoroutineContext
                get() = newCoroutineContext

            override fun resumeWith(result: Result<T>) {
                continuation.resumeWith(result)
            }
        }
        block.startCoroutine(newContinuation)
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

    sealed class ProjectConfigurationResult {
        object Success : ProjectConfigurationResult()
        data class Failure(val failures: List<Throwable>) : ProjectConfigurationResult()
    }

    val project: Project

    val stage: Stage

    fun enqueue(stage: Stage, action: KotlinPluginLifecycle.() -> Unit)

    fun launch(block: suspend KotlinPluginLifecycle.() -> Unit)

    suspend fun await(stage: Stage)

    fun <T : Any> newProperty(
        type: Class<T>, finaliseIn: Stage, initialValue: T?,
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
    private val isFinishedSuccessfully = AtomicBoolean(false)
    private val isFinishedWithFailures = AtomicBoolean(false)

    override var stage: Stage = Stage.values.first()
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
        assert(isStarted.get())
        assert(!isFinishedWithFailures.getAndSet(true))
        project.configurationResultImpl.complete(ProjectConfigurationResult.Failure(failures))
    }

    private fun finishSuccessfully() {
        assert(isStarted.get())
        assert(!isFinishedSuccessfully.getAndSet(true))
        project.configurationResultImpl.complete(ProjectConfigurationResult.Success)
    }

    override fun enqueue(stage: Stage, action: KotlinPluginLifecycle.() -> Unit) {
        if (stage < this.stage) {
            throw IllegalLifecycleException("Cannot enqueue Action for stage '$stage' in current stage '${this.stage}'")
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

    override fun <T : Any> newProperty(type: Class<T>, finaliseIn: Stage, initialValue: T?): LifecycleAwareProperty<T> {
        val property = project.objects.property(type)
        if (initialValue != null) property.set(initialValue)
        if (finaliseIn <= stage) property.finalizeValue()
        else enqueue(finaliseIn) { property.finalizeValue() }
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
        override val property: Property<T>,
    ) : LifecycleAwareProperty<T> {

        override suspend fun awaitFinalValue(): T? {
            finaliseIn.await()
            return property.orNull
        }
    }
}

private class KotlinPluginLifecycleCoroutineContextElement(
    val lifecycle: KotlinPluginLifecycle,
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

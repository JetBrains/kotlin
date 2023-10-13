/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.*
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.projectStoredProperty
import kotlin.coroutines.*

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
internal fun Project.launch(
    start: CoroutineStart = CoroutineStart.Default,
    block: suspend KotlinPluginLifecycle.() -> Unit,
) {
    kotlinPluginLifecycle.launch(start, block)
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
internal val Project.kotlinPluginLifecycle: KotlinPluginLifecycle by projectStoredProperty {
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
    get() = (kotlinPluginLifecycle as KotlinPluginLifecycleImpl).configurationResult


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
    return coroutineContext.kotlinPluginLifecycle
}

/**
 * Suspends execution until we *at least* reached the specified [this@await]
 * This will return right away if the specified [this@await] was already executed or we are currently executing the [this@await]
 */
internal suspend fun Stage.await() {
    currentKotlinPluginLifecycle().await(this)
}

/**
 * Will suspend until [Stage.FinaliseDsl], finalise the value using [Property.finalizeValue] and return the
 * final value.
 */
internal suspend fun <T : Any> Property<T>.awaitFinalValue(): T? {
    Stage.AfterFinaliseDsl.await()
    finalizeValue()
    return orNull
}

/**
 * Will suspend until [Stage.FinaliseDsl], finalise the value using [Property.finalizeValue] and return the
 * final value or throw if value wasn't set.
 */
internal suspend fun <T : Any> Property<T>.awaitFinalValueOrThrow(): T {
    Stage.AfterFinaliseDsl.await()
    finalizeValue()
    return orNull ?: throw IllegalLifecycleException("Property has no value available: ${currentKotlinPluginLifecycle()}")
}

/**
 * See also [withRestrictedStages]
 *
 * Will ensure that the given [block] can only execute in the given [stage]
 * Will wait for the given [stage] if not arrived yet
 */
internal suspend fun <T> requiredStage(stage: Stage, block: suspend () -> T): T {
    if (currentKotlinPluginLifecycle().stage < stage) stage.await()
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

    enum class CoroutineStart {
        /**
         * Puts a coroutine at the end of the current execution queue
         */
        Default,

        /**
         * Immediately executes the coroutine until its first suspension point in the current thread
         */
        Undispatched
    }

    sealed class ProjectConfigurationResult {
        object Success : ProjectConfigurationResult()
        data class Failure(val failures: List<Throwable>) : ProjectConfigurationResult()
    }

    val project: Project

    val stage: Stage

    val isStarted: Boolean

    val isFinished: Boolean

    fun launch(
        start: CoroutineStart = CoroutineStart.Default,
        block: suspend KotlinPluginLifecycle.() -> Unit,
    )

    suspend fun await(stage: Stage)

    class IllegalLifecycleException(message: String) : IllegalStateException(message)
}

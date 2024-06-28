/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.IllegalLifecycleException
import kotlin.coroutines.*

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
internal suspend fun <T> withRestrictedStages(allowed: Set<KotlinPluginLifecycle.Stage>, block: suspend () -> T): T {
    val newCoroutineContext = coroutineContext + KotlinPluginLifecycleStageRestriction(currentKotlinPluginLifecycle(), allowed)
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

private class KotlinPluginLifecycleStageRestriction(
    private val lifecycle: KotlinPluginLifecycle,
    private val allowedStages: Set<KotlinPluginLifecycle.Stage>,
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

/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalStdlibApi::class)

package kotlin.coroutines.jvm.internal

import java.util.ServiceLoader
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.jvm.DebugProbes

private val debugProbes: Array<DebugProbes> = run {
    /*
    Respect debug probes loading explicitly being enabled or disabled by the System Property.
    If the property is not specified, we disable the mechanics on Android as using Service Loaders on Android can
    have unintended performance effects, especially at startup where this code might run.
     */
    val isDebugProbesLoadingEnabled = System.getProperty("kotlin.coroutines.jvm.DebugProbes")?.toBooleanStrictOrNull()
        ?: runCatching { Class.forName("android.os.Build") }.isFailure

    if (isDebugProbesLoadingEnabled) {
        ServiceLoader.load(DebugProbes::class.java, DebugProbes::class.java.classLoader).toList().toTypedArray()
    } else emptyArray()
}


/**
 * This probe is invoked when coroutine is being created and it can replace completion
 * with its own wrapped object to intercept completion of this coroutine.
 *
 * This probe is invoked from stdlib implementation of [createCoroutineUnintercepted] function.
 *
 * Once created, coroutine is repeatedly [resumed][probeCoroutineResumed] and [suspended][probeCoroutineSuspended],
 * until it is complete. On completion, the object that was returned by this probe is invoked.
 *
 * ```
 * +-------+  probeCoroutineCreated +-----------+
 * | START | ---------------------->| SUSPENDED |
 * +-------+                        +-----------+
 *                                      |   ^
 *                probeCoroutineResumed |   | probeCoroutineSuspended
 *                              +-------+   |
 *                              |       |   |
 *                              |       V   |
 *                              |   +------------+ completion invoked  +-----------+
 *                              +-- |   RUNNING  | ------------------->| COMPLETED |
 *                                  +------------+                     +-----------+
 * ```
 *
 * While the coroutine is resumed and suspended, it is represented by the pointer to its `frame`
 * which always extends [BaseContinuationImpl] and represents a pointer to the topmost frame of the
 * coroutine. Each [BaseContinuationImpl] object has [completion][BaseContinuationImpl.completion] reference
 * that points either to another frame (extending [BaseContinuationImpl]) or to the completion object
 * that was returned by this `probeCoroutineCreated` function.
 *
 * When coroutine is [suspended][probeCoroutineSuspended], then it is later [resumed][probeCoroutineResumed]
 * with a reference to the same frame. However, while coroutine is running it can unwind its frames and
 * invoke other suspending functions, so its next suspension can happen with a different frame pointer.
 */
@SinceKotlin("1.3")
internal fun <T> probeCoroutineCreated(completion: Continuation<T>): Continuation<T> {
    /** implementation of this function can be replaced by debugger */
    if (debugProbes.isEmpty()) return completion
    return debugProbes.foldRight(completion) { probe, acc -> probe.probeCoroutineCreated(acc) }
}

/**
 * This probe is invoked when coroutine is resumed using [Continuation.resumeWith].
 *
 * This probe is invoked from stdlib implementation of [BaseContinuationImpl.resumeWith] function.
 * Note, this probe can be invoked multiple times when coroutine is running. Every time the coroutine
 * resumes a part its callstack that was previously stored in the heap, this probe is invoked
 * with the references to the newly resumed [frame].
 *
 * Coroutines machinery implementation guarantees that the actual [frame] instance extends
 * [BaseContinuationImpl] class, despite the fact that the declared type of [frame]
 * parameter in this function is `Continuation<*>`. See [probeCoroutineCreated] for details.
 */
@SinceKotlin("1.3")
@Suppress("UNUSED_PARAMETER")
internal fun probeCoroutineResumed(frame: Continuation<*>) {
    /** implementation of this function can be replaced by debugger */
    if (debugProbes.isEmpty()) return
    debugProbes.forEach { it.probeCoroutineResumed(frame) }
}

/**
 * This probe is invoked when coroutine is suspended using [suspendCoroutineUninterceptedOrReturn], that is
 * when the corresponding `block` returns [COROUTINE_SUSPENDED].
 *
 * This probe is invoked from compiler-generated intrinsic for [suspendCoroutineUninterceptedOrReturn] function.
 *
 * Coroutines machinery implementation guarantees that the actual [frame] instance extends
 * [BaseContinuationImpl] class, despite the fact that the declared type of [frame]
 * parameter in this function is `Continuation<*>`. See [probeCoroutineCreated] for details.
 */
@SinceKotlin("1.3")
@Suppress("UNUSED_PARAMETER")
internal fun probeCoroutineSuspended(frame: Continuation<*>) {
    /** implementation of this function can be replaced by debugger */
    if (debugProbes.isEmpty()) return
    debugProbes.forEach { it.probeCoroutineSuspended(frame) }
}

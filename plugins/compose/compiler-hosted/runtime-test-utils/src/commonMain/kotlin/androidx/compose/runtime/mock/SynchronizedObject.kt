/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(ExperimentalContracts::class)

package androidx.compose.runtime.mock

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A [SynchronizedObject] provides a mechanism for thread coordination. Instances of this class are
 * used within [synchronized] functions to establish mutual exclusion, guaranteeing that only one
 * thread accesses a protected resource or code block at a time.
 */
internal expect class SynchronizedObject()

/**
 * Executes the given function [action] while holding the monitor of the given object [lock].
 *
 * The implementation is platform specific:
 * - JVM: implemented via `synchronized`, `ReentrantLock` is avoided for performance reasons.
 * - Native: implemented via POSIX mutex with `PTHREAD_MUTEX_RECURSIVE` flag.
 */
@Suppress("LEAKED_IN_PLACE_LAMBDA", "WRONG_INVOCATION_KIND") // KT-29963
internal inline fun <T> synchronized(lock: SynchronizedObject, crossinline action: () -> T): T {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    return synchronizedImpl(lock, action)
}

/**
 * Executes the given function [action] while holding the monitor of the given object [lock].
 *
 * The implementation is platform specific:
 * - JVM: implemented via `synchronized`, `ReentrantLock` is avoided for performance reasons.
 * - Native: implemented via POSIX mutex with `PTHREAD_MUTEX_RECURSIVE` flag.
 *
 * **This is a private API and should not be used from general code.** This function exists
 * primarily as a workaround for a Kotlin issue
 * ([KT-29963](https://youtrack.jetbrains.com/issue/KT-29963)).
 *
 * You **MUST** use [synchronized] instead.
 */
internal expect inline fun <T> synchronizedImpl(
    lock: SynchronizedObject,
    crossinline action: () -> T,
): T

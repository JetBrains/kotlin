/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin

import kotlin.reflect.KProperty

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer]
 * and the default thread-safety mode [LazyThreadSafetyMode.SYNCHRONIZED].
 *
 * If the initialization of a value throws an exception, it will attempt to reinitialize the value at next access.
 *
 * Note that the returned instance uses itself to synchronize on. Do not synchronize from external code on
 * the returned instance as it may cause accidental deadlock. Also this behavior can be changed in the future.
 */
@FixmeConcurrency
public actual fun <T> lazy(initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)//SynchronizedLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer]
 * and thread-safety [mode].
 *
 * If the initialization of a value throws an exception, it will attempt to reinitialize the value at next access.
 *
 * Note that when the [LazyThreadSafetyMode.SYNCHRONIZED] mode is specified the returned instance uses itself
 * to synchronize on. Do not synchronize from external code on the returned instance as it may cause accidental deadlock.
 * Also this behavior can be changed in the future.
 */
@FixmeConcurrency
public actual fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> =
        when (mode) {
            LazyThreadSafetyMode.SYNCHRONIZED -> TODO()//SynchronizedLazyImpl(initializer)
            LazyThreadSafetyMode.PUBLICATION -> TODO()//SafePublicationLazyImpl(initializer)
            LazyThreadSafetyMode.NONE -> UnsafeLazyImpl(initializer)
        }

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer]
 * and the default thread-safety mode [LazyThreadSafetyMode.SYNCHRONIZED].
 *
 * If the initialization of a value throws an exception, it will attempt to reinitialize the value at next access.
 *
 * The returned instance uses the specified [lock] object to synchronize on.
 * When the [lock] is not specified the instance uses itself to synchronize on,
 * in this case do not synchronize from external code on the returned instance as it may cause accidental deadlock.
 * Also this behavior can be changed in the future.
 */
@FixmeConcurrency
@Suppress("UNUSED_PARAMETER")
public actual fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T> = TODO()//SynchronizedLazyImpl(initializer, lock)

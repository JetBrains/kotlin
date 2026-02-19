/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

import kotlin.native.concurrent.*

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer]
 * and the default thread-safety mode [LazyThreadSafetyMode.SYNCHRONIZED].
 *
 * If the initialization of a value throws an exception, it will attempt to reinitialize the value at next access.
 *
 * Note that the returned instance uses itself to synchronize on. Do not synchronize from external code on
 * the returned instance as it may cause accidental deadlock. This behavior might be changed in the future.
 *
 * @sample samples.lazy.LazySamples.lazySample
 */
@OptIn(kotlin.ExperimentalStdlibApi::class)
public actual fun <T> lazy(initializer: () -> T): Lazy<T> = SynchronizedLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer]
 * and thread-safety [mode].
 *
 * If the initialization of a value throws an exception, it will attempt to reinitialize the value at next access.
 *
 * @sample samples.lazy.LazySamples.lazySynchronizedSample
 * @sample samples.lazy.LazySamples.lazySafePublicationSample
 */
@OptIn(kotlin.ExperimentalStdlibApi::class)
public actual fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> =
        when (mode) {
            LazyThreadSafetyMode.SYNCHRONIZED -> SynchronizedLazyImpl(initializer)
            LazyThreadSafetyMode.PUBLICATION -> SafePublicationLazyImpl(initializer)
            LazyThreadSafetyMode.NONE -> UnsafeLazyImpl(initializer)
        }

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer]
 * and the default thread-safety mode [LazyThreadSafetyMode.SYNCHRONIZED].
 *
 * If the initialization of a value throws an exception, it will attempt to reinitialize the value at next access.
 *
 * The returned instance uses the specified [lock] object to synchronize on.
 */
@Suppress("UNUSED_PARAMETER")
@Deprecated("Synchronization on Any? object is not supported.", ReplaceWith("lazy(initializer)"))
@DeprecatedSinceKotlin(errorSince = "1.9", hiddenSince = "2.1")
public actual fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T> = throw UnsupportedOperationException()
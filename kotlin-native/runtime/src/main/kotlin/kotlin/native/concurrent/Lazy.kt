/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(ExperimentalStdlibApi::class)

package kotlin.native.concurrent

import kotlin.experimental.ExperimentalNativeApi
import kotlin.concurrent.atomics.AtomicReference

internal object UNINITIALIZED

internal object INITIALIZING

@OptIn(ExperimentalNativeApi::class)
internal class AtomicLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    private val initializer_ = AtomicReference<Function0<T>?>(initializer)
    private val value_ = AtomicReference<Any?>(UNINITIALIZED)

    override val value: T
        get() {
            if (value_.compareAndExchange(UNINITIALIZED, INITIALIZING) === UNINITIALIZED) {
                // We execute exclusively here.
                val ctor = initializer_.load()
                if (ctor != null && initializer_.compareAndSet(ctor, null)) {
                    value_.compareAndSet(INITIALIZING, ctor())
                } else {
                    // Something wrong.
                    assert(false)
                }
            }
            var result: Any?
            do {
                result = value_.load()
            } while (result === INITIALIZING)

            assert(result !== UNINITIALIZED && result !== INITIALIZING)
            @Suppress("UNCHECKED_CAST")
            return result as T
        }

    override fun isInitialized(): Boolean = value_.load() !== UNINITIALIZED

    override fun toString(): String = if (isInitialized())
        value_.load().toString() else "Lazy value not initialized yet."
}

/**
 * Atomic lazy initializer, could be used in frozen objects, freezes initializing lambda,
 * so use very carefully. Also, as with other uses of an [AtomicReference] may potentially
 * leak memory, so it is recommended to use `atomicLazy` in cases of objects living forever,
 * such as object singletons, or in cases where it's guaranteed not to have cyclical garbage.
 */
@Deprecated("Support for the legacy memory manager has been completely removed. Use lazy() instead.", ReplaceWith("lazy(initializer)"))
@DeprecatedSinceKotlin(errorSince = "2.1")
public fun <T> atomicLazy(initializer: () -> T): Lazy<T> = AtomicLazyImpl(initializer)

@Suppress("UNCHECKED_CAST")
internal class SynchronizedLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    private var initializer = AtomicReference<(() -> T)?>(initializer)
    private var valueRef = AtomicReference<Any?>(UNINITIALIZED)
    private val lock = Lock()

    override val value: T
        get() {
            val _v1 = valueRef.load()
            if (_v1 !== UNINITIALIZED) {
                return _v1 as T
            }

            return locked(lock) {
                val _v2 = valueRef.load()
                if (_v2 === UNINITIALIZED) {
                    val typedValue = initializer.load()!!()
                    valueRef.store(typedValue)
                    initializer.store(null)
                    typedValue
                } else {
                    _v2 as T
                }
            }
        }

    override fun isInitialized() = valueRef.load() !== UNINITIALIZED

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}


@Suppress("UNCHECKED_CAST")
internal class SafePublicationLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    private var initializer = AtomicReference<(() -> T)?>(initializer)
    private var valueRef = AtomicReference<Any?>(UNINITIALIZED)

    override val value: T
        get() {
            val value = valueRef.load()
            if (value !== UNINITIALIZED) {
                return value as T
            }

            val initializerValue = initializer.load()
            // if we see null in initializer here, it means that the value is already set by another thread
            if (initializerValue != null) {
                val newValue = initializerValue()
                if (valueRef.compareAndSet(UNINITIALIZED, newValue)) {
                    initializer.store(null)
                    return newValue
                }
            }

            return valueRef.load() as T
        }

    override fun isInitialized(): Boolean = valueRef.load() !== UNINITIALIZED

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}

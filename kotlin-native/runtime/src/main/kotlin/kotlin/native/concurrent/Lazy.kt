/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.Frozen

internal class FreezeAwareLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    private val value_ = FreezableAtomicReference<Any?>(UNINITIALIZED)
    private var initializer_: (() -> T)? = initializer
    private val lock_ = Lock()

    private fun getOrInit(doFreeze: Boolean): T {
        var result = value_.value
        if (result !== UNINITIALIZED) {
            if (result === INITIALIZING) {
                value_.value = UNINITIALIZED
                throw IllegalStateException("Recursive lazy computation")
            }
            @Suppress("UNCHECKED_CAST")
            return result as T
        }
        // Set value_ to INITIALIZING.
        value_.value = INITIALIZING
        try {
            result = initializer_!!()
            if (doFreeze) result.freeze()
        } catch (throwable: Throwable) {
            value_.value = UNINITIALIZED
            throw throwable
        }
        if (!doFreeze) {
            if (this.isFrozen) {
                value_.value = UNINITIALIZED
                throw InvalidMutabilityException("Frozen during lazy computation")
            }
            // Clear initializer.
            initializer_ = null
        }
        // Set value_ to actual one.
        value_.value = result
        return result
    }

    override val value: T
        get() {
            return if (isFrozen) {
                locked(lock_) {
                    getOrInit(true)
                }
            } else {
                getOrInit(false)
            }
        }

    /**
     * This operation on shared objects may return value which is no longer reflect the current state of lazy.
     */
    override fun isInitialized(): Boolean = (value_.value !== UNINITIALIZED) && (value_.value !== INITIALIZING)

    override fun toString(): String = if (isInitialized())
        value.toString() else "Lazy value not initialized yet"
}

internal object UNINITIALIZED {
    // So that single-threaded configs can use those as well.
    init {
        freeze()
    }
}

internal object INITIALIZING {
    // So that single-threaded configs can use those as well.
    init {
        freeze()
    }
}

@Frozen
internal class AtomicLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    private val initializer_ = AtomicReference<Function0<T>?>(initializer.freeze())
    private val value_ = AtomicReference<Any?>(UNINITIALIZED)

    override val value: T
        get() {
            if (value_.compareAndSwap(UNINITIALIZED, INITIALIZING) === UNINITIALIZED) {
                // We execute exclusively here.
                val ctor = initializer_.value
                if (ctor != null && initializer_.compareAndSet(ctor, null)) {
                    value_.compareAndSet(INITIALIZING, ctor().freeze())
                } else {
                    // Something wrong.
                    assert(false)
                }
            }
            var result: Any?
            do {
                result = value_.value
            } while (result === INITIALIZING)

            assert(result !== UNINITIALIZED && result !== INITIALIZING)
            @Suppress("UNCHECKED_CAST")
            return result as T
        }

    override fun isInitialized(): Boolean = value_.value !== UNINITIALIZED

    override fun toString(): String = if (isInitialized())
        value_.value.toString() else "Lazy value not initialized yet."
}

/**
 * Atomic lazy initializer, could be used in frozen objects, freezes initializing lambda,
 * so use very carefully. Also, as with other uses of an [AtomicReference] may potentially
 * leak memory, so it is recommended to use `atomicLazy` in cases of objects living forever,
 * such as object signletons, or in cases where it's guaranteed not to have cyclical garbage.
 */
public fun <T> atomicLazy(initializer: () -> T): Lazy<T> = AtomicLazyImpl(initializer)

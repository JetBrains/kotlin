/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.Frozen
import kotlin.native.internal.NoReorderFields

@SymbolName("Konan_ensureAcyclicAndSet")
private external fun ensureAcyclicAndSet(where: Any, index: Int, what: Any?): Boolean

@SymbolName("ReadHeapRefNoLock")
internal external fun readHeapRefNoLock(where: Any, index: Int): Any?

@NoReorderFields
internal class FreezeAwareLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    // IMPORTANT: due to simplified ensureAcyclicAndSet() semantics fields here must be ordered like this,
    // as an ordinal is used to refer a field.
    private var value_: Any? = UNINITIALIZED
    private var initializer_: (() -> T)? = initializer
    private val lock_ = Lock()

    override val value: T
        get() {
            if (isFrozen) {
                locked(lock_) {
                    // Lock is already taken above.
                    var result = readHeapRefNoLock(this, 0)
                    if (result !== UNINITIALIZED) {
                        assert(result !== INITIALIZING)
                        @Suppress("UNCHECKED_CAST")
                        return result as T
                    }
                    // Set value_ to INITIALIZING.
                    ensureAcyclicAndSet(this, 0, INITIALIZING)
                    result = initializer_!!().freeze()
                    // Set value_.
                    if (!ensureAcyclicAndSet(this, 0, result)) {
                        throw InvalidMutabilityException("Setting cyclic data via lazy in $this: $result")
                    }
                    // Do not clear initializer_ reference, as it may break freezing invariants and zero out
                    // still valid object. It seems to be safe only in case when `this` is not reachable from
                    // initializer.
                    @Suppress("UNCHECKED_CAST")
                    return result as T
                }
            } else {
                var result: Any? = value_
                if (result === UNINITIALIZED) {
                    result = initializer_!!()
                    if (isFrozen)
                        throw InvalidMutabilityException("$this got frozen during lazy evaluation" )
                    value_ = result
                    initializer_ = null
                }
                @Suppress("UNCHECKED_CAST")
                return result as T
            }
        }

    /**
     * This operation on shared objects may return value which is no longer reflect the current state of lazy.
     */
    override fun isInitialized(): Boolean = (value_ !== UNINITIALIZED) && (value_ !== INITIALIZING)

    override fun toString(): String = if (isInitialized())
        value.toString() else "Lazy value not initialized yet."
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
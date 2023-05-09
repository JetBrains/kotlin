/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:Suppress("DEPRECATION")
@file:OptIn(ExperimentalForeignApi::class)
package kotlin.native.concurrent

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.internal.Frozen
import kotlin.concurrent.AtomicReference
import kotlinx.cinterop.ExperimentalForeignApi

@FreezingIsDeprecated

internal class FreezeAwareLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    private val value_ = FreezableAtomicReference<Any?>(UNINITIALIZED)
    // This cannot be made atomic because of the legacy MM. See https://github.com/JetBrains/kotlin-native/pull/3944
    // So it must be protected by the lock below.
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
            return if (isShareable()) {
                // TODO: This is probably a big performance problem for lazy with the new MM. Address it.
                locked(lock_) {
                    getOrInit(isFrozen)
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

@OptIn(FreezingIsDeprecated::class)
internal object UNINITIALIZED {
    // So that single-threaded configs can use those as well.
    init {
        freeze()
    }
}

@OptIn(FreezingIsDeprecated::class)
internal object INITIALIZING {
    // So that single-threaded configs can use those as well.
    init {
        freeze()
    }
}

@OptIn(ExperimentalNativeApi::class)
@FreezingIsDeprecated
@Frozen
internal class AtomicLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    private val initializer_ = AtomicReference<Function0<T>?>(initializer.freeze())
    private val value_ = AtomicReference<Any?>(UNINITIALIZED)

    override val value: T
        get() {
            if (value_.compareAndExchange(UNINITIALIZED, INITIALIZING) === UNINITIALIZED) {
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
 * such as object singletons, or in cases where it's guaranteed not to have cyclical garbage.
 */
@FreezingIsDeprecated
public fun <T> atomicLazy(initializer: () -> T): Lazy<T> = AtomicLazyImpl(initializer)

@Suppress("UNCHECKED_CAST")
@OptIn(FreezingIsDeprecated::class)
internal class SynchronizedLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    private var initializer = FreezableAtomicReference<(() -> T)?>(initializer)
    private var valueRef = FreezableAtomicReference<Any?>(UNINITIALIZED)
    private val lock = Lock()

    override val value: T
        get() {
            val _v1 = valueRef.value
            if (_v1 !== UNINITIALIZED) {
                return _v1 as T
            }

            return locked(lock) {
                val _v2 = valueRef.value
                if (_v2 === UNINITIALIZED) {
                    val wasFrozen = this.isFrozen
                    val typedValue = initializer.value!!()
                    if (this.isFrozen) {
                        if (!wasFrozen) {
                            throw InvalidMutabilityException("Frozen during lazy computation")
                        }
                        typedValue.freeze()
                    }
                    valueRef.value = typedValue
                    initializer.value = null
                    typedValue
                } else {
                    _v2 as T
                }
            }
        }

    override fun isInitialized() = valueRef.value !== UNINITIALIZED

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}


@Suppress("UNCHECKED_CAST")
@OptIn(FreezingIsDeprecated::class)
internal class SafePublicationLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    private var initializer = FreezableAtomicReference<(() -> T)?>(initializer)
    private var valueRef = FreezableAtomicReference<Any?>(UNINITIALIZED)

    override val value: T
        get() {
            val value = valueRef.value
            if (value !== UNINITIALIZED) {
                return value as T
            }

            val initializerValue = initializer.value
            // if we see null in initializer here, it means that the value is already set by another thread
            if (initializerValue != null) {
                val wasFrozen = this.isFrozen
                val newValue = initializerValue()
                if (this.isFrozen) {
                    if (!wasFrozen) {
                        throw InvalidMutabilityException("Frozen during lazy computation")
                    }
                    newValue.freeze()
                }
                if (valueRef.compareAndSet(UNINITIALIZED, newValue)) {
                    initializer.value = null
                    return newValue
                }
            }
            return valueRef.value as T
        }

    override fun isInitialized(): Boolean = valueRef.value !== UNINITIALIZED

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}

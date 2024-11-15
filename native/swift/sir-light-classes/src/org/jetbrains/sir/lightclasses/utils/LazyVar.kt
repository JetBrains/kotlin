/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.KProperty2

internal interface LazyVar<T> : Lazy<T> {
    @Suppress("UNCHECKED_CAST", "NO_REFLECTION_IN_CLASS_PATH")
    companion object {
        fun <V> reset(property: KProperty0<V>): V? =
            (property.getDelegate() as? LazyVar<V>)?.reset()

        fun <U, V> reset(property: KProperty1<U, V>, receiver: U): V? =
            (property.getDelegate(receiver) as? LazyVar<V>)?.reset()

        fun <T, U, V> reset(property: KProperty2<T, U, V>, receiver1: T, receiver2: U): V? =
            (property.getDelegate(receiver1, receiver2) as? LazyVar<V>)?.reset()
    }

    override var value: T

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) { value = newValue }

    fun reset(): T?
}

internal fun <T> lazyVar(initializer: () -> T): LazyVar<T> = SynchronizedMutableLazyImpl(initializer)

private object UNINITIALIZED_VALUE

private class SynchronizedMutableLazyImpl<T>(initializer: () -> T, lock: Any? = null) : LazyVar<T> {
    private var initializer: (() -> T)? = initializer
    @Volatile private var _value: Any? = UNINITIALIZED_VALUE

    // final field is required to enable safe publication of constructed instance
    private val lock = lock ?: this

    override var value: T
        get() {
            val _v1 = _value
            if (_v1 !== UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                return _v1 as T
            }

            return synchronized(lock) {
                val _v2 = _value
                if (_v2 !== UNINITIALIZED_VALUE) {
                    @Suppress("UNCHECKED_CAST") (_v2 as T)
                } else {
                    val typedValue = initializer!!()
                    _value = typedValue
                    initializer = null
                    typedValue
                }
            }
        }
        set(newValue) {
            synchronized(lock) {
                _value = newValue
            }
        }

    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun reset(): T? {
        var oldValue: T?
        synchronized(lock) {
            @Suppress("UNCHECKED_CAST")
            oldValue = _value.takeIf { it != UNINITIALIZED_VALUE } as T?
            _value = UNINITIALIZED_VALUE
        }
        return oldValue
    }

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}

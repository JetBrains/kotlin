/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import kotlin.reflect.KProperty

internal interface LazyVar<T> : Lazy<T> {
    override var value: T

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) { value = newValue }
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

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}

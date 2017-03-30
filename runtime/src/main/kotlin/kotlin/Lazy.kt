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
 * Represents a value with lazy initialization.
 *
 * To create an instance of [Lazy] use the [lazy] function.
 */
public interface Lazy<out T> {
    /**
     * Gets the lazily initialized value of the current Lazy instance.
     * Once the value was initialized it must not change during the rest of lifetime of this Lazy instance.
     */
    public val value: T
    /**
     * Returns `true` if a value for this Lazy instance has been already initialized, and `false` otherwise.
     * Once this function has returned `true` it stays `true` for the rest of lifetime of this Lazy instance.
     */
    public fun isInitialized(): Boolean
}

/**
 * Creates a new instance of the [Lazy] that is already initialized with the specified [value].
 */
public fun <T> lazyOf(value: T): Lazy<T> = InitializedLazyImpl(value)

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
public fun <T> lazy(initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)//SynchronizedLazyImpl(initializer)

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
public fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> =
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
public fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T> = TODO()//SynchronizedLazyImpl(initializer, lock)

/**
 * An extension to delegate a read-only property of type [T] to an instance of [Lazy].
 *
 * This extension allows to use instances of Lazy for property delegation:
 * `val property: String by lazy { initializer }`
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> Lazy<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

/**
 * Specifies how a [Lazy] instance synchronizes access among multiple threads.
 */
public enum class LazyThreadSafetyMode {

    /**
     * Locks are used to ensure that only a single thread can initialize the [Lazy] instance.
     */
    SYNCHRONIZED,

    /**
     * Initializer function can be called several times on concurrent access to uninitialized [Lazy] instance value,
     * but only first returned value will be used as the value of [Lazy] instance.
     */
    PUBLICATION,

    /**
     * No locks are used to synchronize the access to the [Lazy] instance value; if the instance is accessed from multiple threads, its behavior is undefined.
     *
     * This mode should be used only when high performance is crucial and the [Lazy] instance is guaranteed never to be initialized from more than one thread.
     */
    NONE,
}


private object UNINITIALIZED_VALUE

//private class SynchronizedLazyImpl<out T>(initializer: () -> T, lock: Any? = null) : Lazy<T>, Serializable {
//    private var initializer: (() -> T)? = initializer
//    @Volatile private var _value: Any? = UNINITIALIZED_VALUE
//    // final field is required to enable safe publication of constructed instance
//    private val lock = lock ?: this
//
//    override val value: T
//        get() {
//            val _v1 = _value
//            if (_v1 !== UNINITIALIZED_VALUE) {
//                @Suppress("UNCHECKED_CAST")
//                return _v1 as T
//            }
//
//            return synchronized(lock) {
//                val _v2 = _value
//                if (_v2 !== UNINITIALIZED_VALUE) {
//                    @Suppress("UNCHECKED_CAST") (_v2 as T)
//                }
//                else {
//                    val typedValue = initializer!!()
//                    _value = typedValue
//                    initializer = null
//                    typedValue
//                }
//            }
//        }
//
//    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE
//
//    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
//
//    private fun writeReplace(): Any = InitializedLazyImpl(value)
//}

internal class UnsafeLazyImpl<out T>(initializer: () -> T) : Lazy<T>/*, Serializable*/ {
    private var initializer: (() -> T)? = initializer
    private var _value: Any? = UNINITIALIZED_VALUE

    override val value: T
        get() {
            if (_value === UNINITIALIZED_VALUE) {
                _value = initializer!!()
                initializer = null
            }
            @Suppress("UNCHECKED_CAST")
            return _value as T
        }

    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    private fun writeReplace(): Any = InitializedLazyImpl(value)
}

private class InitializedLazyImpl<out T>(override val value: T) : Lazy<T>/*, Serializable*/ {

    override fun isInitialized(): Boolean = true

    override fun toString(): String = value.toString()

}

//private class SafePublicationLazyImpl<out T>(initializer: () -> T) : Lazy<T>, Serializable {
//    private var initializer: (() -> T)? = initializer
//    @Volatile private var _value: Any? = UNINITIALIZED_VALUE
//    // this final field is required to enable safe publication of constructed instance
//    private val final: Any = UNINITIALIZED_VALUE
//
//    override val value: T
//        get() {
//            if (_value === UNINITIALIZED_VALUE) {
//                val initializerValue = initializer
//                // if we see null in initializer here, it means that the value is already set by another thread
//                if (initializerValue != null) {
//                    val newValue = initializerValue()
//                    if (valueUpdater.compareAndSet(this, UNINITIALIZED_VALUE, newValue)) {
//                        initializer = null
//                    }
//                }
//            }
//            @Suppress("UNCHECKED_CAST")
//            return _value as T
//        }
//
//    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE
//
//    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
//
//    private fun writeReplace(): Any = InitializedLazyImpl(value)
//
//    companion object {
//        private val valueUpdater = java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater(
//                SafePublicationLazyImpl::class.java,
//                Any::class.java,
//                "_value")
//    }
//}
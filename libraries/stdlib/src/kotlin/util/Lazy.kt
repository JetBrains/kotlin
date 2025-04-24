/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmName("LazyKt")
@file:kotlin.jvm.JvmMultifileClass

package kotlin

import kotlin.internal.ReadObjectParameterType
import kotlin.internal.throwReadObjectNotSupported
import kotlin.reflect.KProperty

/**
 * Represents a value with lazy initialization.
 *
 * To create an instance of [Lazy] use the [lazy] function.
 *
 * @sample samples.lazy.LazySamples.lazySample
 */
public interface Lazy<out T> {
    /**
     * Gets the lazily initialized value of the current `Lazy` instance.
     * Once the value was initialized it must not change during the rest of lifetime of this `Lazy` instance.
     *
     * @sample samples.lazy.LazySamples.lazyExplicitSample
     */
    public val value: T

    /**
     * Returns `true` if a value for this `Lazy` instance has been already initialized, and `false` otherwise.
     * Once this function has returned `true` it stays `true` for the rest of lifetime of this `Lazy` instance.
     */
    public fun isInitialized(): Boolean
}

/**
 * Creates a new instance of the [Lazy] that is already initialized with the specified [value].
 */
public fun <T> lazyOf(value: T): Lazy<T> = InitializedLazyImpl(value)

/**
 * An extension to delegate a read-only property of type [T] to an instance of [Lazy].
 *
 * This extension allows to use instances of Lazy for property delegation:
 * `val property: String by lazy { initializer }`
 *
 * @sample samples.lazy.LazySamples.lazySample
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> Lazy<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

/**
 * Specifies how a [Lazy] instance synchronizes initialization and publication among multiple threads.
 * On platforms with no notion of synchronization and threads (JS and WASM), all modes are considered equal
 * to the default implementation.
 *
 * @see lazy
 */
public enum class LazyThreadSafetyMode {

    /**
     * Uses a lock to ensure that only a single thread can initialize a [Lazy] instance,
     * and ensures that initialized value is visible by all threads.
     * The lock used is both platform- and implementation- specific detail.
     *
     * @sample samples.lazy.LazySamples.lazySynchronizedSample
     */
    SYNCHRONIZED,

    /**
     * Initializer function can be called several times on concurrent access to an uninitialized [Lazy] instance value,
     * but only one computed value will be used as the value of a [Lazy] instance and will be visible by all threads.
     *
     * @sample samples.lazy.LazySamples.lazySafePublicationSample
     */
    PUBLICATION,

    /**
     * No locks are used to synchronize access and initialization of a [Lazy] instance value.
     * If the instance is accessed from multiple threads, its behavior is unspecified.
     *
     * This mode should not be used unless the [Lazy] instance is guaranteed never to be initialized from more than one thread.
     */
    NONE,
}


internal object UNINITIALIZED_VALUE

// internal to be called from lazy in JS
internal class UnsafeLazyImpl<out T>(initializer: () -> T) : Lazy<T>, Serializable {
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

    private fun readObject(input: ReadObjectParameterType): Unit = throwReadObjectNotSupported()
}

internal class InitializedLazyImpl<out T>(override val value: T) : Lazy<T>, Serializable {

    override fun isInitialized(): Boolean = true

    override fun toString(): String = value.toString()

}

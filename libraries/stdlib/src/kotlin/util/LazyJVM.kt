@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("LazyKt")

package kotlin

import java.io.Serializable


/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer]
 * and the default thread-safety mode [LazyThreadSafetyMode.SYNCHRONIZED].
 *
 * If the initialization of a value throws an exception, it will attempt to reinitialize the value at next access.
 *
 * Note that the returned instance uses itself to synchronize on. Do not synchronize from external code on
 * the returned instance as it may cause accidental deadlock. Also this behavior can be changed in the future.
 */
public fun lazy<T>(initializer: () -> T): Lazy<T> = SynchronizedLazyImpl(initializer)

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
public fun lazy<T>(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> =
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
 * When the [lock] is not specified the instance uses itself to synchronize on,
 * in this case do not synchronize from external code on the returned instance as it may cause accidental deadlock.
 * Also this behavior can be changed in the future.
 */
public fun lazy<T>(lock: Any?, initializer: () -> T): Lazy<T> = SynchronizedLazyImpl(initializer, lock)


private class SafePublicationLazyImpl<out T>(initializer: () -> T) : Lazy<T>(), Serializable {
    private var initializer: (() -> T)? = initializer
    @Volatile private var _value: Any? = UNINITIALIZED_VALUE
    private val final: Any = UNINITIALIZED_VALUE

    override val value: T
        get() {
            if (_value === UNINITIALIZED_VALUE) {
                val newValue = initializer!!()
                if (valueUpdater.compareAndSet(this, UNINITIALIZED_VALUE, newValue)) {
                    initializer == null
                }
            }
            return _value as T
        }

    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    private fun writeReplace(): Any = InitializedLazyImpl(value)

    companion object {
        private val valueUpdater = java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater(
                SafePublicationLazyImpl::class.java,
                Any::class.java,
                "_value")
    }
}
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("LazyKt")

package kotlin

import java.io.Serializable


/**
 * Represents a value with lazy initialization.
 *
 * To create an instance of [Lazy] use the [lazy] function.
 */
public abstract class Lazy<out T> internal constructor() {
    /** Gets the lazily initialized value of the current Lazy instance. */
    public abstract  val value: T
    /** Returns `true` if a value for this Lazy instance has been already initialized, and `false` otherwise.
     *  Once this function has returned `true` it stays `true` for the rest of lifetime of this Lazy instance.
     */
    public abstract fun isInitialized(): Boolean
}

/**
 * Creates a new instance of the [Lazy] that is already initialized with the specified [value].
 */
public fun lazyOf<T>(value: T): Lazy<T> = InitializedLazyImpl(value)

/**
 * An extension to delegate a read-only property of type [T] to an instance of [Lazy].
 *
 * This extension allows to use instances of Lazy for property delegation:
 * `val property: String by lazy { initializer }`
 */
public fun <T> Lazy<T>.get(thisRef: Any?, property: PropertyMetadata): T = value

/**
 * Specifies how a [Lazy] instance synchronizes access among multiple threads.
 */
public enum class LazyThreadSafetyMode {

    /**
     * Locks are used to ensure that only a single thread can initialize the [Lazy] instance.
     */
    SYNCHRONIZED,

    /**
     * No locks are used to synchronize the access to the [Lazy] instance value; if the instance is accessed from multiple threads, its behavior is undefined.
     *
     * This mode should be used only when high performance is crucial and the [Lazy] instance is guaranteed never to be initialized from more than one thread.
     */
    NONE,
}


private object UNINITIALIZED_VALUE

internal open class LazyImpl<out T>(initializer: () -> T) : Lazy<T>(), Serializable {
    private var initializer: (() -> T)? = initializer
    @Volatile private var _value: Any? = UNINITIALIZED_VALUE
    protected open val lock: Any
        get() = this

    override val value: T
        get() {
            val _v1 = _value
            if (_v1 !== UNINITIALIZED_VALUE) {
                return _v1 as T
            }

            return synchronized(lock) {
                val _v2 = _value
                if (_v2 !== UNINITIALIZED_VALUE) {
                     _v2 as T
                }
                else {
                    val typedValue = initializer!!()
                    _value = typedValue
                    initializer = null
                    typedValue
                }
            }
        }

    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    private fun writeReplace(): Any = InitializedLazyImpl(value)
}

internal class ExternallySynchronizedLazyImpl<out T>(override val lock: Any, initializer: () -> T): LazyImpl<T>(initializer)

internal class UnsafeLazyImpl<out T>(initializer: () -> T) : Lazy<T>(), Serializable {
    private var initializer: (() -> T)? = initializer
    private var _value: Any? = UNINITIALIZED_VALUE

    override val value: T
        get() {
            if (_value === UNINITIALIZED_VALUE) {
                _value = initializer!!()
                initializer == null
            }
            return _value as T
        }

    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    private fun writeReplace(): Any = InitializedLazyImpl(value)
}

private class InitializedLazyImpl<out T>(override val value: T) : Lazy<T>(), Serializable {

    override fun isInitialized(): Boolean = true

    override fun toString(): String = value.toString()

}

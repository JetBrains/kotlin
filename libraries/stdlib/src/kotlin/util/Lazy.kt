package kotlin

import java.io.Serializable


/**
 * Represents a value with lazy initialization.
 */
public interface Lazy<out T> {
    /** Gets the lazily initialized value of the current Lazy instance. */
    public val value: T
    /** Returns `true` if a value for this Lazy instance has been already initialized. */
    public val valueCreated: Boolean
}

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

private class LazyImpl<out T>(initializer: () -> T) : Lazy<T>, Serializable {
    private var initializer: (() -> T)? = initializer
    private volatile var _value: Any? = UNINITIALIZED_VALUE

    override val value: T
        get() {
            val _v1 = _value
            if (_v1 !== UNINITIALIZED_VALUE) {
                return _v1 as T
            }

            return synchronized(this) {
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

    override val valueCreated: Boolean get() = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (valueCreated) value.toString() else "Lazy value not initialized yet."

    private fun writeReplace(): Any = InitializedLazyImpl(value)
}


private class UnsafeLazyImpl<out T>(initializer: () -> T) : Lazy<T>, Serializable {
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

    override val valueCreated: Boolean get() = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (valueCreated) value.toString() else "Lazy value not initialized yet."

    private fun writeReplace(): Any = InitializedLazyImpl(value)
}

private class InitializedLazyImpl<out T>(override val value: T) : Lazy<T>, Serializable {

    override val valueCreated: Boolean get() = true

    override fun toString(): String = value.toString()

}

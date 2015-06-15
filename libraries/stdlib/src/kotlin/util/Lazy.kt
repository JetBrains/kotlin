package kotlin


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


private object NULL_VALUE {}

private fun escape(value: Any?): Any {
    return value ?: NULL_VALUE
}

private fun unescape(value: Any?): Any? {
    return if (value === NULL_VALUE) null else value
}


private class LazyImpl<out T>(initializer: () -> T, private val lockObj: Any) : Lazy<T> {
    private var initializer: (() -> T)? = initializer
    private volatile var _value: Any? = null

    public override val value: T
        get() {
            val _v1 = _value
            if (_v1 != null) {
                return unescape(_v1) as T
            }

            return synchronized(lockObj) {
                val _v2 = _value
                if (_v2 != null) {
                     unescape(_v2) as T
                }
                else {
                    val typedValue = initializer!!()
                    _value = escape(typedValue)
                    initializer = null
                    typedValue
                }
            }
        }

    override val valueCreated: Boolean = _value == null

    override fun toString(): String = if (valueCreated) value.toString() else "Lazy value not created yet."
}


private class UnsafeLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    private var initializer: (() -> T)? = initializer


    private var _value: Any? = null

    override val value: T
        get() {
            if (_value == null) {
                _value = escape(initializer!!())
                initializer == null
            }
            return unescape(_value) as T
        }

    override val valueCreated: Boolean = _value == null

    override fun toString(): String = if (valueCreated) value.toString() else "Lazy value not created yet."
}

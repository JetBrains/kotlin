package kotlin



public interface Lazy<out T> {
    public val value: T
    public val valueCreated: Boolean
}

public fun <T> Lazy<T>.get(thisRef: Any?, property: PropertyMetadata): T = value


public enum class LazyThreadSafetyMode {
    SYNCHRONIZED,
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

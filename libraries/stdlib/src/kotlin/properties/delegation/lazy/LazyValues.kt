package kotlin.properties.delegation.lazy

private val NULL_VALUE: Any = Any()

private fun <T> escape(value: T): Any {
    return if (value == null) NULL_VALUE else value
}

private fun <T: Any> unescape(value: Any): T? {
    return if (value == NULL_VALUE) null else value as T
}

public open class LazyVal<T: Any>(initializer: () -> T): NullableLazyVal<T>(initializer) {
    override fun get(thisRef: Any?, desc: PropertyMetadata): T {
        return super.get(thisRef, desc)!!
    }
}

public open class NullableLazyVal<T: Any>(private val initializer: () -> T?) {
    private var value: Any? = null

    public open fun get(thisRef: Any?, desc: PropertyMetadata): T? {
        if (value == null) {
            value = escape(initializer())
        }
        return unescape(value!!)
    }
}

public open class VolatileLazyVal<T: Any>(initializer: () -> T): VolatileNullableLazyVal<T>(initializer) {
    override fun get(thisRef: Any?, desc: PropertyMetadata): T {
        return super.get(thisRef, desc)!!
    }
}

public open class VolatileNullableLazyVal<T: Any>(private val initializer: () -> T?) {
    private volatile var value: Any? = null

    public open fun get(thisRef: Any?, desc: PropertyMetadata): T? {
        if (value == null) {
            value = escape(initializer())
        }
        return unescape(value!!)
    }
}

public open class AtomicLazyVal<T: Any>(lock: Any? = null, initializer: () -> T): AtomicNullableLazyVal<T>(lock, initializer) {
    override fun get(thisRef: Any?, desc: PropertyMetadata): T {
        return super.get(thisRef, desc)!!
    }
}

public open class AtomicNullableLazyVal<T: Any>(lock: Any? = null, private val initializer: () -> T?) {
    private val lock = lock ?: this
    private volatile var value: Any? = null

    public open fun get(thisRef: Any?, desc: PropertyMetadata): T? {
        val _v1 = value
        if (_v1 != null) {
            return unescape(_v1)
        }

        return synchronized(lock) {
            val _v2 = value
            if (_v2 != null) {
                unescape<T>(_v2)
            }
            else {
                val typedValue = initializer()
                value = escape(typedValue)
                typedValue
            }
        }
    }
}
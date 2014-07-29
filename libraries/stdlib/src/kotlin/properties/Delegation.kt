package kotlin.properties

public trait ReadOnlyProperty<in R, out T> {
    public fun get(thisRef: R, desc: PropertyMetadata): T
}

public trait ReadWriteProperty<in R, T> {
    public fun get(thisRef: R, desc: PropertyMetadata): T
    public fun set(thisRef: R, desc: PropertyMetadata, value: T)
}

public object Delegates {
    public fun notNull<T: Any>(): ReadWriteProperty<Any?, T> = NotNullVar()

    public fun lazy<T>(initializer: () -> T): ReadOnlyProperty<Any?, T> = LazyVal(initializer)
    public fun blockingLazy<T>(lock: Any? = null, initializer: () -> T): ReadOnlyProperty<Any?, T> = BlockingLazyVal(lock, initializer)

    public fun observable<T>(initial: T, onChange: (desc: PropertyMetadata, oldValue: T, newValue: T) -> Unit): ReadWriteProperty<Any?, T> {
        return ObservableProperty<T>(initial) { (desc, old, new) ->
            onChange(desc, old, new)
            true
        }
    }

    public fun vetoable<T>(initial: T, onChange: (desc: PropertyMetadata, oldValue: T, newValue: T) -> Boolean): ReadWriteProperty<Any?, T> {
        return ObservableProperty<T>(initial, onChange)
    }

    public fun mapVar<T>(map: MutableMap<in String, Any?>,
                         default: (thisRef: Any?, desc: String) -> T = defaultValueProvider): ReadWriteProperty<Any?, T> {
        return FixedMapVar<Any?, String, T>(map, defaultKeyProvider, default)
    }

    public fun mapVal<T>(map: Map<in String, Any?>,
                         default: (thisRef: Any?, desc: String) -> T = defaultValueProvider): ReadOnlyProperty<Any?, T> {
        return FixedMapVal<Any?, String, T>(map, defaultKeyProvider, default)
    }
}


private class NotNullVar<T: Any>() : ReadWriteProperty<Any?, T> {
    private var value: T? = null

    public override fun get(thisRef: Any?, desc: PropertyMetadata): T {
        return value ?: throw IllegalStateException("Property ${desc.name} should be initialized before get")
    }

    public override fun set(thisRef: Any?, desc: PropertyMetadata, value: T) {
        this.value = value
    }
}

public class ObservableProperty<T>(
        initialValue: T, private val onChange: (name: PropertyMetadata, oldValue: T, newValue: T) -> Boolean
) : ReadWriteProperty<Any?, T> {
    private var value = initialValue

    public override fun get(thisRef: Any?, desc: PropertyMetadata): T {
        return value
    }

    public override fun set(thisRef: Any?, desc: PropertyMetadata, value: T) {
        if (onChange(desc, this.value, value)) {
            this.value = value
        }
    }
}

private val NULL_VALUE: Any = Any()

private fun escape(value: Any?): Any {
    return value ?: NULL_VALUE
}

private fun unescape(value: Any?): Any? {
    return if (value == NULL_VALUE) null else value
}

private class LazyVal<T>(private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {
    private var value: Any? = null

    public override fun get(thisRef: Any?, desc: PropertyMetadata): T {
        if (value == null) {
            value = escape(initializer())
        }
        return unescape(value) as T
    }
}

private class BlockingLazyVal<T>(lock: Any?, private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {
    private val lock = lock ?: this
    private volatile var value: Any? = null

    public override fun get(thisRef: Any?, desc: PropertyMetadata): T {
        val _v1 = value
        if (_v1 != null) {
            return unescape(_v1) as T
        }

        return synchronized(lock) {
            val _v2 = value
            if (_v2 != null) {
                unescape(_v2) as T
            }
            else {
                val typedValue = initializer()
                value = escape(typedValue)
                typedValue
            }
        }
    }
}

public class KeyMissingException(message: String): RuntimeException(message)

public abstract class MapVal<T, in K, out V>() : ReadOnlyProperty<T, V> {
    protected abstract fun map(ref: T): Map<in K, Any?>
    protected abstract fun key(desc: PropertyMetadata): K

    protected open fun default(ref: T, desc: PropertyMetadata): V {
        throw KeyMissingException("Key $desc is missing in $ref")
    }

    public override fun get(thisRef: T, desc: PropertyMetadata) : V {
        val map = map(thisRef)
        val key = key(desc)
        if (!map.containsKey(key)) {
            return default(thisRef, desc)
        }

        return map[key] as V
    }
}

public abstract class MapVar<T, in K, V>() : MapVal<T, K, V>(), ReadWriteProperty<T, V> {
    protected abstract override fun map(ref: T): MutableMap<in K, Any?>

    public override fun set(thisRef: T, desc: PropertyMetadata, value: V) {
        val map = map(thisRef)
        map.put(key(desc), value)
    }
}

private val defaultKeyProvider:(PropertyMetadata) -> String = {it.name}
private val defaultValueProvider:(Any?, Any?) -> Nothing = {(thisRef, key) -> throw KeyMissingException("$key is missing from $thisRef")}

public open class FixedMapVal<T, in K, out V>(private val map: Map<in K, Any?>,
                                              private val key: (PropertyMetadata) -> K,
                                              private val default: (ref: T, key: K) -> V = defaultValueProvider) : MapVal<T, K, V>() {
    protected override fun map(ref: T): Map<in K, Any?> {
        return map
    }

    protected override fun key(desc: PropertyMetadata): K {
        return (key)(desc)
    }

    protected override fun default(ref: T, desc: PropertyMetadata): V {
        return (default)(ref, key(desc))
    }
}

public open class FixedMapVar<T, in K, V>(private val map: MutableMap<in K, Any?>,
                                          private val key: (PropertyMetadata) -> K,
                                          private val default: (ref: T, key: K) -> V = defaultValueProvider) : MapVar<T, K, V>() {
    protected override fun map(ref: T): MutableMap<in K, Any?> {
        return map
    }

    protected override fun key(desc: PropertyMetadata): K {
        return (key)(desc)
    }

    protected override fun default(ref: T, desc: PropertyMetadata): V {
        return (default)(ref, key(desc))
    }
}

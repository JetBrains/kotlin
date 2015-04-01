package kotlin.properties

/**
 * Base trait that can be used for implementing property delegates of read-only properties. This is provided only for
 * convenience; you don't have to extend this trait as long as your property delegate has methods with the same
 * signatures.
 * @param R the type of object which owns the delegated property.
 * @param T the type of the property value.
 */
public trait ReadOnlyProperty<in R, out T> {
    /**
     * Returns the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param desc the metadata for the property.
     * @return the property value.
     */
    public fun get(thisRef: R, desc: PropertyMetadata): T
}

/**
 * Base trait that can be used for implementing property delegates of read-only properties. This is provided only for
 * convenience; you don't have to extend this trait as long as your property delegate has methods with the same
 * signatures.
 * @param R the type of object which owns the delegated property.
 * @param T the type of the property value.
 */
public trait ReadWriteProperty<in R, T> {
    /**
     * Returns the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param desc the metadata for the property.
     * @return the property value.
     */
    public fun get(thisRef: R, desc: PropertyMetadata): T

    /**
     * Sets the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param desc the metadata for the property.
     * @param value the value to set.
     */
    public fun set(thisRef: R, desc: PropertyMetadata, value: T)
}

/**
 * Standard property delegates.
 */
public object Delegates {
    /**
     * Returns a property delegate for a read/write property with a non-null value that is initialized not during
     * object construction time but at a later time. Trying to read the property before the initial value has been
     * assigned results in an exception.
     */
    public fun notNull<T: Any>(): ReadWriteProperty<Any?, T> = NotNullVar()

    /**
     * Returns a property delegate for a read-only property that is initialized on first access by calling the
     * specified block of code. Supports lazy initialization semantics for properties.
     * @param initializer the function that returns the value of the property.
     */
    public fun lazy<T>(initializer: () -> T): ReadOnlyProperty<Any?, T> = LazyVal(initializer)

    /**
     * Returns a property delegate for a read-only property that is initialized on first access by calling the
     * specified block of code under a specified lock. Supports lazy initialization semantics for properties and
     * concurrent access.
     * @param lock the object the monitor of which is locked before calling the initializer block. If not specified,
     *             the property delegate object itself is used as a lock.
     * @param initializer the function that returns the value of the property.
     */
    public fun blockingLazy<T>(lock: Any? = null, initializer: () -> T): ReadOnlyProperty<Any?, T> = BlockingLazyVal(lock, initializer)

    /**
     * Returns a property delegate for a read/write property that calls a specified callback function when changed.
     * @param initial the initial value of the property.
     * @param onChange the callback which is called when the property value is changed.
     */
    public fun observable<T>(initial: T, onChange: (desc: PropertyMetadata, oldValue: T, newValue: T) -> Unit): ReadWriteProperty<Any?, T> {
        return ObservableProperty<T>(initial) { desc, old, new ->
            onChange(desc, old, new)
            true
        }
    }

    /**
     * Returns a property delegate for a read/write property that calls a specified callback function when changed,
     * allowing the callback to veto the modification.
     * @param initial the initial value of the property.
     * @param onChange the callback which is called when a change to the property value is attempted. The new value
    *                  is saved if the callback returns true and discarded if the callback returns false.
     */
    public fun vetoable<T>(initial: T, onChange: (desc: PropertyMetadata, oldValue: T, newValue: T) -> Boolean): ReadWriteProperty<Any?, T> {
        return ObservableProperty<T>(initial, onChange)
    }

    /**
     * Returns a property delegate for a read/write property that stores its value in a map, using the property name
     * as a key.
     * @param map the map where the property values are stored.
     * @param default the function returning the value of the property for a given object if it's missing from the given map.
     */
    public fun mapVar<T>(map: MutableMap<in String, Any?>,
                         default: (thisRef: Any?, desc: String) -> T = defaultValueProvider): ReadWriteProperty<Any?, T> {
        return FixedMapVar<Any?, String, T>(map, defaultKeyProvider, default)
    }

    /**
     * Returns a property delegate for a read-only property that takes its value from a map, using the property name
     * as a key.
     * @param map the map where the property values are stored.
     * @param default the function returning the value of the property for a given object if it's missing from the given map.
     */
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

/**
 * Implements a property delegate for a read/write property that calls a specified callback function when changed.
 * @param initialValue the initial value of the property.
 * @param onChange the callback which is called when a change to the property value is attempted. The new value
*                  is saved if the callback returns true and discarded if the callback returns false.
 */
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

private object NULL_VALUE {}

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

/**
 * Exception thrown by the default implementation of property delegates which store values in a map
 * when the map does not contain the corresponding key.
 */
public class KeyMissingException(message: String): RuntimeException(message)

/**
 * Implements the core logic for a property delegate that stores property values in a map.
 * @param T the type of the object that owns the delegated property.
 * @param K the type of key in the map.
 * @param V the type of the property value.
 */
public abstract class MapVal<T, K, out V>() : ReadOnlyProperty<T, V> {
    /**
     * Returns the map used to store the values of the properties of the given object instance.
     * @param ref the object instance for which the map is requested.
     */
    protected abstract fun map(ref: T): Map<in K, Any?>

    /**
     * Returns the map key used to store the values of the given property.
     * @param desc the property for which the key is requested.
     */
    protected abstract fun key(desc: PropertyMetadata): K

    /**
     * Returns the property value to be used when the map does not contain the corresponding key.
     * @param ref the object instance for which the value was requested.
     * @param desc the property for which the value was requested.
     */
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

/**
 * Implements the core logic for a read/write property delegate that stores property values in a map.
 * @param T the type of the object that owns the delegated property.
 * @param K the type of key in the map.
 * @param V the type of the property value.
 */
public abstract class MapVar<T, K, V>() : MapVal<T, K, V>(), ReadWriteProperty<T, V> {
    protected abstract override fun map(ref: T): MutableMap<in K, Any?>

    public override fun set(thisRef: T, desc: PropertyMetadata, value: V) {
        val map = map(thisRef)
        map.put(key(desc), value)
    }
}

private val defaultKeyProvider:(PropertyMetadata) -> String = {it.name}
private val defaultValueProvider:(Any?, Any?) -> Nothing = {thisRef, key -> throw KeyMissingException("$key is missing from $thisRef")}

/**
 * Implements a read-only property delegate that stores the property values in a given map instance and uses the given
 * callback functions for calculating the key and the default value for each property.
 * @param map the map used to store the values.
 * @param key the function to calculate the map key from a property metadata object.
 * @param default the function returning the value of the property for a given object if it's missing from the given map.
 */
public open class FixedMapVal<T, K, out V>(private val map: Map<in K, Any?>,
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

/**
 * Implements a read/write property delegate that stores the property values in a given map instance and uses the given
 * callback functions for calculating the key and the default value for each property.
 * @param map the map used to store the values.
 * @param key the function to calculate the map key from a property metadata object.
 * @param default the function returning the value of the property for a given object if it's missing from the given map.
 */
public open class FixedMapVar<T, K, V>(private val map: MutableMap<in K, Any?>,
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

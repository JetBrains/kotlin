package kotlin.properties.delegation

import kotlin.properties.ChangeSupport

public class NotNullVar<T: Any> {
    private var value: T? = null

    public fun get(thisRef: Any?, desc: PropertyMetadata): T {
        return value ?: throw IllegalStateException("Property ${desc.name} should be initialized before get")
    }

    public fun set(thisRef: Any?, desc: PropertyMetadata, value: T) {
        this.value = value
    }
}

public class SynchronizedVar<T>(private val initValue: T, private val lock: Any) {
    private var value: T = initValue

    public fun get(thisRef: Any?, desc: PropertyMetadata): T {
        return synchronized(lock) { value }
    }

    public fun set(thisRef: Any?, desc: PropertyMetadata, newValue: T) {
        synchronized(lock) {
            value = newValue
        }
    }
}

class ObservableProperty<T>(initialValue: T, val changeSupport: (name: String, oldValue: T, newValue: T) -> Unit) {
    private var value = initialValue

    public fun get(thisRef: Any?, desc: PropertyMetadata): T {
        return value
    }

    public fun set(thisRef: Any?, desc: PropertyMetadata, newValue: T) {
        changeSupport(desc.name, value, newValue)
        value = newValue
    }
}

public class KeyMissingException(message: String): RuntimeException(message)

public abstract class MapVal<T, V> {
    public abstract fun getMap(thisRef: T): Map<*, *>
    public abstract fun getKey(desc: PropertyMetadata): Any?

    public open fun getDefaultValue(desc: PropertyMetadata, key: Any?): V {
        throw KeyMissingException("Key $key is missing")
    }

    public fun get(thisRef: T, desc: PropertyMetadata): V {
        val key = getKey(desc)
        val map = getMap(thisRef)
        if (!map.containsKey(key)) {
            return getDefaultValue(desc, key)
        }
        return map[key] as V
    }
}

public abstract class MapVar<T, V>: MapVal<T, V>() {

    public fun set(thisRef: T, desc: PropertyMetadata, newValue: V) {
        val map = getMap(thisRef) as MutableMap<Any?, Any?>
        map[getKey(desc)] = newValue
    }
}

public fun <V> Map<String, *>.readOnlyProperty(default: (() -> V)? = null): MapVal<Any?, V> {
    return object: MapVal<Any?, V>() {
        override fun getMap(thisRef: Any?) = this@readOnlyProperty
        override fun getKey(desc: PropertyMetadata) = desc.name
        override fun getDefaultValue(desc: PropertyMetadata, key: Any?) = if (default == null) super.getDefaultValue(desc, key) else default()
    }
}

public fun <V> Map<*, *>.readOnlyProperty(default: (() -> V)? = null, key: Any?): MapVal<Any?, V> {
    return object: MapVal<Any?, V>() {
        override fun getMap(thisRef: Any?) = this@readOnlyProperty
        override fun getKey(desc: PropertyMetadata) = key
        override fun getDefaultValue(desc: PropertyMetadata, key: Any?) = if (default == null) super.getDefaultValue(desc, key) else default()
    }
}

public fun <V> Map<*, *>.readOnlyProperty(default: (() -> V)? = null, key: (desc: PropertyMetadata) -> Any?): MapVal<Any?, V> {
    return object: MapVal<Any?, V>() {
        override fun getMap(thisRef: Any?) = this@readOnlyProperty
        override fun getKey(desc: PropertyMetadata) = key(desc)
        override fun getDefaultValue(desc: PropertyMetadata, key: Any?) = if (default == null) super.getDefaultValue(desc, key) else default()
    }
}

public fun <V> MutableMap<String, *>.property(default: (() -> V)? = null): MapVar<Any?, V> {
    return object: MapVar<Any?, V>() {
        override fun getMap(thisRef: Any?) = this@property
        override fun getKey(desc: PropertyMetadata) = desc.name
        override fun getDefaultValue(desc: PropertyMetadata, key: Any?) = if (default == null) super.getDefaultValue(desc, key) else default()
    }
}

public fun <V> MutableMap<*, *>.property(default: (() -> V)? = null, key: Any?): MapVar<Any?, V> {
    return object: MapVar<Any?, V>() {
        override fun getMap(thisRef: Any?) = this@property
        override fun getKey(desc: PropertyMetadata) = key
        override fun getDefaultValue(desc: PropertyMetadata, key: Any?) = if (default == null) super.getDefaultValue(desc, key) else default()
    }
}

public fun <V> MutableMap<*, *>.property(default: (() -> V)? = null, key: (desc: PropertyMetadata) -> Any?): MapVar<Any?, V> {
    return object: MapVar<Any?, V>() {
        override fun getMap(thisRef: Any?) = this@property
        override fun getKey(desc: PropertyMetadata) = key(desc)
        override fun getDefaultValue(desc: PropertyMetadata, key: Any?) = if (default == null) super.getDefaultValue(desc, key) else default()
    }
}
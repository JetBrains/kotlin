@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MapsKt")

package kotlin.collections

import java.util.*

/**
 * Returns the value for the given key, or the implicit default value for this map.
 * By default no implicit value is provided for maps and a [NoSuchElementException] is thrown.
 * To create a map with implicit default value use [withDefault] method.
 *
 * @throws NoSuchElementException when the map doesn't contain value for the specified key and no implicit default was provided for that map.
 */
@kotlin.jvm.JvmName("getOrImplicitDefaultNullable")
@kotlin.internal.InlineExposed
internal fun <K, V> Map<K, V>.getOrImplicitDefault(key: K): V {
    if (this is MapWithDefault)
        return this.getOrImplicitDefault(key)

    return getOrElseNullable(key, { throw NoSuchElementException("Key $key is missing in the map.") })
}

/**
 * Returns a wrapper of this read-only map, having the implicit default value provided with the specified function [defaultValue].
 * This implicit default value is used when properties are delegated to the returned map,
 * and that map doesn't contain value for the key specified.
 *
 * When this map already have an implicit default value provided with a former call to [withDefault], it is being replaced by this call.
 */
public fun <K, V> Map<K, V>.withDefault(defaultValue: (key: K) -> V): Map<K, V> =
        when (this) {
            is MapWithDefault -> this.map.withDefault(defaultValue)
            else -> MapWithDefaultImpl(this, defaultValue)
        }

/**
 * Returns a wrapper of this mutable map, having the implicit default value provided with the specified function [defaultValue].
 * This implicit default value is used when properties are delegated to the returned map,
 * and that map doesn't contain value for the key specified.
 *
 * When this map already have an implicit default value provided with a former call to [withDefault], it is being replaced by this call.
 */
@kotlin.jvm.JvmName("withDefaultMutable")
public fun <K, V> MutableMap<K, V>.withDefault(defaultValue: (key: K) -> V): MutableMap<K, V> =
        when (this) {
            is MutableMapWithDefault -> this.map.withDefault(defaultValue)
            else -> MutableMapWithDefaultImpl(this, defaultValue)
        }





private interface MapWithDefault<K, out V>: Map<K, V> {
    public val map: Map<K, V>
    public fun getOrImplicitDefault(key: K): V
}

private interface MutableMapWithDefault<K, V>: MutableMap<K, V>, MapWithDefault<K, V> {
    public override val map: MutableMap<K, V>
}


private class MapWithDefaultImpl<K, out V>(public override val map: Map<K,V>, private val default: (key: K) -> V) : MapWithDefault<K, V> {
    override fun equals(other: Any?): Boolean = map.equals(other)
    override fun hashCode(): Int = map.hashCode()
    override fun toString(): String = map.toString()
    override val size: Int get() = map.size
    override fun isEmpty(): Boolean = map.isEmpty()
    override fun containsKey(key: K): Boolean = map.containsKey(key)
    override fun containsValue(value: @UnsafeVariance V): Boolean = map.containsValue(value)
    override fun get(key: K): V? = map.get(key)
    override val keys: Set<K> get() = map.keys
    override val values: Collection<V> get() = map.values
    override val entries: Set<Map.Entry<K, V>> get() = map.entries

    override fun getOrImplicitDefault(key: K): V = map.getOrElseNullable(key, { default(key) })
}

private class MutableMapWithDefaultImpl<K, V>(public override val map: MutableMap<K, V>, private val default: (key: K) -> V): MutableMapWithDefault<K, V> {
    override fun equals(other: Any?): Boolean = map.equals(other)
    override fun hashCode(): Int = map.hashCode()
    override fun toString(): String = map.toString()
    override val size: Int get() = map.size
    override fun isEmpty(): Boolean = map.isEmpty()
    override fun containsKey(key: K): Boolean = map.containsKey(key)
    override fun containsValue(value: @UnsafeVariance V): Boolean = map.containsValue(value)
    override fun get(key: K): V? = map.get(key)
    override val keys: MutableSet<K> get() = map.keys
    override val values: MutableCollection<V> get() = map.values
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = map.entries

    override fun put(key: K, value: V): V? = map.put(key, value)
    override fun remove(key: K): V? = map.remove(key)
    override fun putAll(from: Map<out K, V>) = map.putAll(from)
    override fun clear() = map.clear()

    override fun getOrImplicitDefault(key: K): V = map.getOrElseNullable(key, { default(key) })
}


@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MapsKt")

package kotlin

import java.util.*
import kotlin.platform.platformName

/**
 * Returns the value for the given key, or the implicit default value for this map.
 * By default no implicit value is provided for maps and a [NoSuchElementException] is thrown.
 * To create a map with implicit default value use [withDefault] method.
 *
 * @throws NoSuchElementException when the map doesn't contain value for the specified key and no implicit default was provided for that map.
 */
public fun <K, V> Map<K, V>.getOrImplicitDefault(key: K): V {
    if (this is MapWithDefault)
        return this.getOrImplicitDefault(key)

    return getOrElse(key, { throw NoSuchElementException("Key $key is missing in the map.") })
}

/**
 * Returns a wrapper of this read-only map, having the implicit default value provided with the specified function [default].
 * This implicit default value is used when [getOrImplicitDefault] is called on the returned map,
 * and that map doesn't contain value for the key specified.
 *
 * When this map already have an implicit default value provided with a former call to [withDefault], it is being replaced by this call.
 */
public fun <K, V> Map<K, V>.withDefault(default: (key: K) -> V): Map<K, V> =
        when (this) {
            is MapWithDefault -> this.map.withDefault(default)
            else -> MapWithDefaultImpl(this, default)
        }

/**
 * Returns a wrapper of this mutable map, having the implicit default value provided with the specified function [default].
 * This implicit default value is used when [getOrImplicitDefault] is called on the returned map,
 * and that map doesn't contain value for the key specified.
 *
 * When this map already have an implicit default value provided with a former call to [withDefault], it is being replaced by this call.
 */
@platformName("withDefaultMutable")
public fun <K, V> MutableMap<K, V>.withDefault(default: (key: K) -> V): MutableMap<K, V> =
        when (this) {
            is MutableMapWithDefault -> this.map.withDefault(default)
            else -> MutableMapWithDefaultImpl(this, default)
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
    override fun size(): Int = map.size()
    override fun isEmpty(): Boolean = map.isEmpty()
    override fun containsKey(key: Any?): Boolean = map.containsKey(key)
    override fun containsValue(value: Any?): Boolean = map.containsValue(value)
    override fun get(key: Any?): V? = map.get(key)
    override fun keySet(): Set<K> = map.keySet()
    override fun values(): Collection<V> = map.values()
    override fun entrySet(): Set<Map.Entry<K, V>> = map.entrySet()

    override fun getOrImplicitDefault(key: K): V = map.getOrElse(key, { default(key) })
}

private class MutableMapWithDefaultImpl<K, V>(public override val map: MutableMap<K, V>, private val default: (key: K) -> V): MutableMapWithDefault<K, V> {
    override fun equals(other: Any?): Boolean = map.equals(other)
    override fun hashCode(): Int = map.hashCode()
    override fun toString(): String = map.toString()
    override fun size(): Int = map.size()
    override fun isEmpty(): Boolean = map.isEmpty()
    override fun containsKey(key: Any?): Boolean = map.containsKey(key)
    override fun containsValue(value: Any?): Boolean = map.containsValue(value)
    override fun get(key: Any?): V? = map.get(key)
    override fun keySet(): MutableSet<K> = map.keySet()
    override fun values(): MutableCollection<V> = map.values()
    override fun entrySet(): MutableSet<MutableMap.MutableEntry<K, V>> = map.entrySet()

    override fun put(key: K, value: V): V? = map.put(key, value)
    override fun remove(key: Any?): V? = map.remove(key)
    override fun putAll(m: Map<out K, V>) = map.putAll(m)
    override fun clear() = map.clear()

    override fun getOrImplicitDefault(key: K): V = map.getOrElse(key, { default(key) })
}


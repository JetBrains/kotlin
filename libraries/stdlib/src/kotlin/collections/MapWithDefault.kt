package kotlin

import kotlin.platform.platformName


/**
 * Exception thrown when the map does not contain the corresponding key.
 */
public class KeyMissingException(message: String): RuntimeException(message)


//private inline fun <K, V> Map<K, V>.getOrDefault(key: K, defaultValue: () -> V): V {
//    val value = get(key)
//    if (value == null && !containsKey(key)) {
//        return defaultValue()
//    } else {
//        return value as V
//    }
//}

public fun <K, V> Map<K, V>.getOrImplicitDefault(key: K): V {
    if (this is MapWithDefault)
        return this.getOrImplicitDefault(key)

    return getOrElse(key, { throw KeyMissingException("Key $key is missing in the map.") })
}

public fun <K, V> Map<K, V>.withDefault(default: (key: K) -> V): Map<K, V> =
        when (this) {
            is MapWithDefault -> this.map.withDefault(default)
            else -> MapWithDefaultImpl(this, default)
        }

platformName("withDefaultMutable")
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


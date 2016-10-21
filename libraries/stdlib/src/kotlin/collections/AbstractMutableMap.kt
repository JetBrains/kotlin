@file:JvmVersion
package kotlin.collections

import java.util.AbstractMap

/**
 * Provides a skeletal implementation of the [MutableMap] interface.
 *
 * The implementor is required to implement [entries] property, which should return mutable set of map entries, and [put] function.
 *
 * @param K the type of map keys. The map is invariant on its key type.
 * @param V the type of map values. The map is invariant on its value type.
 */
@SinceKotlin("1.1")
public abstract class AbstractMutableMap<K, V> protected constructor() : MutableMap<K, V>, AbstractMap<K, V>() {
    /**
     * Associates the specified [value] with the specified [key] in the map.
     *
     * This method is redeclared as abstract, because it's not implemented in the base class,
     * so it must be always overridden in the concrete mutable collection implementation.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    abstract override fun put(key: K, value: V): V?
}
package kotlin.collections

/**
 * A collection that holds pairs of objects (keys and values) and supports efficiently retrieving
 * the value corresponding to each key. Map keys are unique; the map holds only one value for each key.
 * Methods in this interface support only read-only access to the map; read-write access is supported through
 * the [MutableMap] interface.
 * @param K the type of map keys.
 * @param V the type of map values.
 */
public interface Map<K, out V> {
    // Query Operations
    /**
     * Returns the number of key/value pairs in the map.
     */
    public val size: Int

    /**
     * Returns `true` if the map is empty (contains no elements), `false` otherwise.
     */
    public fun isEmpty(): Boolean

    /**
     * Returns `true` if the map contains the specified [key].
     */
    public fun containsKey(key: K): Boolean

    /**
     * Returns `true` if the map maps one or more keys to the specified [value].
     */
    public fun containsValue(value: @UnsafeVariance V): Boolean

    /**
     * Returns the value corresponding to the given [key], or `null` if such a key is not present in the map.
     */
    public operator fun get(key: K): V?

    // Views
    /**
     * Returns a [Set] of all keys in this map.
     */
    public val keys: Set<K>

    /**
     * Returns a [Collection] of all values in this map. Note that this collection may contain duplicate values.
     */
    public val values: Collection<V>

    /**
     * Returns a [Set] of all key/value pairs in this map.
     */
    public val entries: Set<Map.Entry<K, V>>

    /**
     * Represents a key/value pair held by a [Map].
     */
    public interface Entry<out K, out V> {
        /**
         * Returns the key of this key/value pair.
         */
        public val key: K

        /**
         * Returns the value of this key/value pair.
         */
        public val value: V
    }
}

/**
 * A modifiable collection that holds pairs of objects (keys and values) and supports efficiently retrieving
 * the value corresponding to each key. Map keys are unique; the map holds only one value for each key.
 * @param K the type of map keys.
 * @param V the type of map values.
 */
public interface MutableMap<K, V> : Map<K, V> {
    // Modification Operations
    /**
     * Associates the specified [value] with the specified [key] in the map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    public fun put(key: K, value: V): V?

    /**
     * Removes the specified key and its corresponding value from this map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    public fun remove(key: K): V?

    // Bulk Modification Operations
    /**
     * Updates this map with key/value pairs from the specified map [from].
     */
    public fun putAll(from: Map<out K, V>): Unit

    /**
     * Removes all elements from this map.
     */
    public fun clear(): Unit

    // Views
    override val keys: MutableSet<K>
    override val values: MutableCollection<V>
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>

    /**
     * Represents a key/value pair held by a [MutableMap].
     */
    public interface MutableEntry<K,V>: Map.Entry<K, V> {
        /**
         * Changes the value associated with the key of this entry.
         *
         * @return the previous value corresponding to the key.
         */
        public fun setValue(newValue: V): V
    }
}

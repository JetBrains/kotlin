@file:Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
@file:JvmName("CollectionsJRE8Kt")
package kotlin.collections

/**
 * Returns the value to which the specified key is mapped, or
 * [defaultValue] if this map contains no mapping for the key.
 */
@SinceKotlin("1.1")
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes K, V> Map<out K, V>.getOrDefault(key: K, defaultValue: V): V
        = (this as Map<K, V>).getOrDefault(key, defaultValue)


/**
 * Removes the entry for the specified key only if it is currently
 * mapped to the specified value.
 */
@SinceKotlin("1.1")
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes K, @kotlin.internal.OnlyInputTypes V> MutableMap<out K, out V>.remove(key: K, value: V): Boolean
        = (this as MutableMap<K, V>).remove(key, value)



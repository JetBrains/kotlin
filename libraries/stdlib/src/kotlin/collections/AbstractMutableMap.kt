@file:JvmVersion
package kotlin.collections

import java.util.AbstractMap

@SinceKotlin("1.1")
public abstract class AbstractMutableMap<K, V> protected constructor() : MutableMap<K, V>, AbstractMap<K, V>() {
    abstract override fun put(key: K, value: V): V?
}
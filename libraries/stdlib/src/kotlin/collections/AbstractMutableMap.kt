@file:JvmVersion
package kotlin.collections

import java.util.AbstractMap

public abstract class AbstractMutableMap<K, V> protected constructor() : AbstractMap<K, V>() {
    abstract override fun put(key: K, value: V): V?
}
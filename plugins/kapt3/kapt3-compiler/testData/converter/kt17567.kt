// IGNORE_BACKEND: JVM_IR

package test

internal class MutableEntry<K, V>(
        private val internal: MutableMap<K, V>,
        override val key: K, value: V
): MutableMap.MutableEntry<K, V>

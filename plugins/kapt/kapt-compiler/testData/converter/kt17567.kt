package test

internal class MutableEntry<K, V>(
        private val internal: MutableMap<K, V>,
        override val key: K, value: V
): MutableMap.MutableEntry<K, V>

// <K:Ljava/lang/Object;V:Ljava/lang/Object;>
// Ljava/lang/Object;
// Ljava/util/Map$Entry<TK;TV;>;
// Lkotlin/jvm/internal/markers/KMutableMap$Entry;

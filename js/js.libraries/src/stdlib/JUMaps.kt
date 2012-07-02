package kotlin

import java.util.Map as JMap
/** Provides [] access to maps */
public fun <K, V> JMap<K, V>.set(key : K, value : V): Unit = this.put(key, value)

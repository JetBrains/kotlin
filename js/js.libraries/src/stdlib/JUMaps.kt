package kotlin

import java.util.Map as JMap
/** Provides [] access to maps */
fun <K, V> JMap<K, V>.set(key : K, value : V) = this.put(key, value)

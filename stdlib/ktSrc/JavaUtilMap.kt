package std.util

import java.util.Map as JMap

// Map APIs

/** Returns the size of the map */
val JMap<*,*>.size : Int
    get() = size()

/** Returns true if this map is empty */
val JMap<*,*>.empty : Boolean
    get() = isEmpty()

/** Returns the value for the given key or returns the result of the defaultValue function if there was no entry for the given key */
inline fun <K,V> java.util.Map<K,V>.getOrElse(key: K, defaultValue: ()-> V) : V {
  val current = this.get(key)
  if (current != null) {
    return current
  } else {
    return defaultValue()
  }
}

/** Returns the value for the given key or the result of the defaultValue function is put into the map for the given value and returned */
inline fun <K,V> java.util.Map<K,V>.getOrElseUpdate(key: K, defaultValue: ()-> V) : V {
  val current = this.get(key)
  if (current != null) {
    return current
  } else {
    val answer = defaultValue()
    this.put(key, answer)
    return answer
  }
}

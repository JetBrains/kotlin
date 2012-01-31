package js

import java.util.*;

class Json() {
    fun set(paramName : String, value : Any?) {}
    fun get(paramName : String) : Any? = null
}

fun <K, V> Map<K, V>.toJson() : Json = Json()
fun json(vararg pairs : Tuple2<String, Any?>) = Json()

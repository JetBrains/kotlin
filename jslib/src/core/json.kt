package js

import java.util.*;
import js.annotations.LibraryFun

class Json() {

}

LibraryFun("jsonSet")
fun Json.set(paramName : String, value : Any?) : Unit {}
LibraryFun("jsonGet")
fun Json.get(paramName : String) : Any? = null

fun <K, V> Map<K, V>.toJson() : Json = Json()
LibraryFun("jsonFromTuples")
fun json(vararg pairs : Tuple2<String, Any?>) = Json()

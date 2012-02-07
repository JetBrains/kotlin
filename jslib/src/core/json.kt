package js

import java.util.*;
import js.library

class Json() {

}

library("jsonSet")
fun Json.set(paramName : String, value : Any?) : Unit {}
library("jsonGet")
fun Json.get(paramName : String) : Any? = null

fun <K, V> Map<K, V>.toJson() : Json = Json()
library("jsonFromTuples")
fun json(vararg pairs : Tuple2<String, Any?>) = Json()

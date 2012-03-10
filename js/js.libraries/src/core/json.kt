package js

import java.util.*;
import js.library

native
class Json() {

}

library("jsonSet")
fun Json.set(paramName : String, value : Any?) : Unit = js.noImpl

library("jsonGet")
fun Json.get(paramName : String) : Any? = js.noImpl

library("jsonFromTuples")
fun json(vararg pairs : Tuple2<String, Any?>) : Json = js.noImpl
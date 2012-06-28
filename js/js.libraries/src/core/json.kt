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

library("jsonFromTuples")
fun json2(pairs : Array<Tuple2<String, Any?>>) : Json = js.noImpl

library("jsonAddProperties")
fun Json.add(other : Json) : Json = js.noImpl

native
trait JsonClass {
    fun stringify(o: Any): String = noImpl
    fun parse<T>(text: String): T = noImpl
}

native
val JSON:JsonClass = noImpl
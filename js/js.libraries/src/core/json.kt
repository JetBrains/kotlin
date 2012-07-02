package js

import java.util.*;
import js.library

native
public class Json() {

}

library("jsonSet")
public fun Json.set(paramName : String, value : Any?) : Unit = js.noImpl

library("jsonGet")
public fun Json.get(paramName : String) : Any? = js.noImpl

library("jsonFromTuples")
public fun json(vararg pairs : Tuple2<String, Any?>) : Json = js.noImpl

library("jsonFromTuples")
public fun json2(pairs : Array<Tuple2<String, Any?>>) : Json = js.noImpl

library("jsonAddProperties")
public fun Json.add(other : Json) : Json = js.noImpl
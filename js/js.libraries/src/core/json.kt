package js

import js.library

native
public class Json() {
    library("jsonGet")
    fun get(parameterName: String): Any?
    library("jsonSet")
    fun set(parameterName: String, value: Any?): Unit
}

library("jsonFromTuples")
public fun json(vararg pairs: Pair<String, Any?>): Json = js.noImpl

library("jsonFromTuples")
public fun json2(pairs: Array<Pair<String, Any?>>): Json = js.noImpl

library("jsonAddProperties")
public fun Json.add(other: Json): Json = js.noImpl

native
public trait JsonClass {
    public fun stringify(o: Any): String
    public fun parse<T>(text: String, reviver: ((key: String, value: Any?)->Unit)? = null): T
}

native
public val JSON: JsonClass = noImpl
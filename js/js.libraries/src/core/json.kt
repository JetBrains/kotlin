package js

import js.library

native public class Json() {
    fun get(propertyName: String): Any? = js.noImpl
    fun set(propertyName: String, value: Any?): Unit = js.noImpl
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
    public fun stringify(o: Any, replacer: (key: String, value: Any?)->Any?): String
    public fun stringify(o: Any, replacer: (key: String, value: Any?)->Any?, space: Int): String
    public fun stringify(o: Any, replacer: (key: String, value: Any?)->Any?, space: String): String
    public fun stringify(o: Any, replacer: Array<String>): String
    public fun stringify(o: Any, replacer: Array<String>, space: Int): String
    public fun stringify(o: Any, replacer: Array<String>, space: String): String

    public fun parse<T>(text: String): T
    public fun parse<T>(text: String, reviver: ((key: String, value: Any?)->Any?)): T
}

native
public val JSON: JsonClass = noImpl
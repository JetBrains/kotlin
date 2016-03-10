package kotlin.js

@native public class Json() {
    operator fun get(propertyName: String): Any? = noImpl
    operator fun set(propertyName: String, value: Any?): Unit = noImpl
}

public fun json(vararg pairs: Pair<String, Any?>): Json {
    val res: dynamic = js("({})")
    for ((name, value) in pairs) {
        res[name] = value
    }
    return res
}

@library("jsonAddProperties")
public fun Json.add(other: Json): Json = noImpl

@native
public interface JsonClass {
    public fun stringify(o: Any): String
    public fun stringify(o: Any, replacer: (key: String, value: Any?) -> Any?): String
    public fun stringify(o: Any, replacer: (key: String, value: Any?) -> Any?, space: Int): String
    public fun stringify(o: Any, replacer: (key: String, value: Any?) -> Any?, space: String): String
    public fun stringify(o: Any, replacer: Array<String>): String
    public fun stringify(o: Any, replacer: Array<String>, space: Int): String
    public fun stringify(o: Any, replacer: Array<String>, space: String): String

    public fun <T> parse(text: String): T
    public fun <T> parse(text: String, reviver: ((key: String, value: Any?) -> Any?)): T
}

@native
public val JSON: JsonClass = noImpl

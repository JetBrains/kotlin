package kotlin.js

public external class Json() {
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

public fun Json.add(other: Json): Json {
    val keys: Array<String> = js("Object").keys(other)
    for (key in keys) {
        if (other.asDynamic().hasOwnProperty(key)) {
            this[key] = other[key];
        }
    }
    return this
}

public external object JSON {
    public fun stringify(o: Any?): String
    public fun stringify(o: Any?, replacer: (key: String, value: Any?) -> Any?): String
    public fun stringify(o: Any?, replacer: (key: String, value: Any?) -> Any?, space: Int): String
    public fun stringify(o: Any?, replacer: (key: String, value: Any?) -> Any?, space: String): String
    public fun stringify(o: Any?, replacer: Array<String>): String
    public fun stringify(o: Any?, replacer: Array<String>, space: Int): String
    public fun stringify(o: Any?, replacer: Array<String>, space: String): String

    public fun <T> parse(text: String): T
    public fun <T> parse(text: String, reviver: ((key: String, value: Any?) -> Any?)): T
}

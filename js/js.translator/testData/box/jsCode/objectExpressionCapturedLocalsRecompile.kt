// FILE: a.kt

import kotlin.js.*

fun isOrdinaryObject(o: Any?): Boolean =
    jsTypeOf(o) == "object" && Object.getPrototypeOf(o).`constructor` === Any::class.js

external class Object {
    companion object {
        fun getPrototypeOf(x: Any?): dynamic
        fun keys(x: Any): Array<String>
    }
}

fun buildObject(): dynamic {
    val one = 1
    val value = 3
    val key = "three"
    val spreadSource = js("{ four: 4 }")

    return js("{ one, [key]: value, ...spreadSource }")
}

// FILE: b.kt
// RECOMPILE

fun box(): String {
    val result = buildObject()

    if (!isOrdinaryObject(result)) return "fail: result is not an object"
    if (Object.keys(result).size != 3) return "fail: result should have three properties"
    if (result.one != 1) return "fail: result.one == ${result.one}"
    if (result["three"] != 3) return "fail: result['three'] == ${result["three"]}"
    if (result.four != 4) return "fail: result.four == ${result.four}"

    return "OK"
}

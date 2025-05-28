// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// ISSUE: KT-77320

package foo

external interface Big {
    fun times(other: Big): String
}

class BigImpl(val value: Int): Big {
    override fun times(other: Big) = "OK"
}

fun createBig(value: Int): dynamic = BigImpl(value)

fun box(): String {
    val a = createBig(1)
    val b = createBig(2)

    val result = a.times(b)
    if (result == "OK") return "OK"

    return "times hasn't been called"
}
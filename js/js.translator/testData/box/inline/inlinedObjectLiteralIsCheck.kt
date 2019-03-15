// EXPECTED_REACHABLE_NODES: 1282
// IGNORE_BACKEND: JS_IR

interface I {
    fun ok(): String
}

inline fun ok(): I {
    return object : I {
        override fun ok() = "OK"
    }
}

@JsName("convolutedOk")
inline fun convolutedOk(): I {
    val fail = object : I {
        override fun ok() = "fail"
    }.ok()

    println(fail)

    return ok()
}

fun box(): String {
    val ok = js("_").convolutedOk()
    if (ok !is I) return "fail"

    return ok.ok()
}
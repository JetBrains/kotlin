// EXPECTED_REACHABLE_NODES: 1282

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

    return ok()
}

fun box(): String {
    val ok = js("_").convolutedOk()
    if (ok !is I) return "fail"

    return ok.ok()
}
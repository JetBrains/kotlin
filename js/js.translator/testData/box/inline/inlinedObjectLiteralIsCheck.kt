// EXPECTED_REACHABLE_NODES: 1282

// DONT_TARGET_EXACT_BACKEND: JS_IR
// REASON: js("_"). is not portable

interface I {
    fun ok(): String
}

inline fun ok(): I {
    return object : I {
        override fun ok() = "OK"
    }
}

@JsName("convolutedOk")
@JsExport
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
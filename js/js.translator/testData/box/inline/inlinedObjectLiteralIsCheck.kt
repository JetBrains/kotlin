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
@JsExport
inline fun convolutedOk(): I {
    val fail = object : I {
        override fun ok() = "fail"
    }.ok()

    return ok()
}

// CHECK_BREAKS_COUNT: function=box count=0
// CHECK_LABELS_COUNT: function=box name=$l$block count=0
fun box(): String {
    val ok = js("_").convolutedOk()
    if (ok !is I) return "fail"

    return ok.ok()
}
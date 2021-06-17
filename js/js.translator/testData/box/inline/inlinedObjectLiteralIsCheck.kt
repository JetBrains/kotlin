// DONT_TARGET_EXACT_BACKEND: JS
// SKIP_OLD_MODULE_SYSTEMS

// FILE: main.kt
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

@JsExport
fun testOk(ok: Any): String {
    if (ok !is I) return "fail"
    return ok.ok()
}

// FILE: entry.mjs
// ENTRY_ES_MODULE
import { convolutedOk, testOk } from "./JS_TESTS/index.js";

console.assert(testOk(convolutedOk()) == "OK");

// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES

// Generated .mjs name is different in Wasm
// DONT_TARGET_EXACT_BACKEND: WASM

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
import { convolutedOk, testOk } from "./inlinedObjectLiteralIsCheck_v5.mjs";

export function box() {
    return testOk(convolutedOk())
}

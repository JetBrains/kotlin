// KT-65657
// DONT_TARGET_EXACT_BACKEND: JS
// SKIP_MINIFICATION
// ES_MODULES

// FILE: main.kt
@JsExport
open class TableDriver {

    private inner class Table

    fun foo() = "OK"
}
external interface JsResult {
    val value: String
    val inner: Any?
}

@JsModule("./privateInnerClass.mjs")
external fun jsBox(): JsResult

fun box(): String {
    val res = jsBox()

    if (res.value != "OK") {
        return "Fail: value is ${res.value}"
    }
    if (res.inner != null) {
        return "Fail: inner is ${res.inner}"
    }

    return "OK"
}

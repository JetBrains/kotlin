// MODULE_KIND: COMMON_JS
// SKIP_MINIFICATION

// FILE: api.kt
package api

@JsExport
interface A {
    companion object {
        fun ok() = "OK"
    }
}

// FILE: main.kt
external interface JsResult {
    val res: String
}

@JsModule("lib")
external fun jsBox(): JsResult

fun box(): String {
    return jsBox().res
}

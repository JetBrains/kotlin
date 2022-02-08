// MODULE_KIND: COMMON_JS
// SKIP_MINIFICATION

// FILE: api.kt
package api
@JsExport
class Something<T: Something<T>> {
    fun ping(): String {
        return "OK"
    }
}

@JsExport
fun ping(s: Something<*>): String {
    return s.ping()
}

// FILE: main.kt
external interface JsResult {
    val pingCall: () -> String
}

@JsModule("lib")
external fun jsBox(): JsResult

fun box(): String {
    return jsBox().pingCall()
}

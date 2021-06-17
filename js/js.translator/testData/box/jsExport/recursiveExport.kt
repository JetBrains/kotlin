// DONT_TARGET_EXACT_BACKEND: JS
// SKIP_MINIFICATION
// SKIP_OLD_MODULE_SYSTEMS

// FILE: api.kt
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

@JsModule("./recursiveExport.mjs")
external fun jsBox(): JsResult

fun box(): String {
    return jsBox().pingCall()
}

// SKIP_MINIFICATION
// ES_MODULES

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

@JsModule("./interfaceWithCompanion.mjs")
external fun jsBox(): JsResult

fun box(): String {
    return jsBox().res
}

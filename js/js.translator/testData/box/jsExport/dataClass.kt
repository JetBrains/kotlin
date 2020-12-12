// MODULE_KIND: COMMON_JS
// SKIP_MINIFICATION

// FILE: api.kt
package api

@JsExport
data class Point(val x: Int, val y: Int) {
    override fun toString(): String = "[${x}::${y}]"
}

// we need his class to make sure that there's more than one ping method in existence - due to peculiarities of current namer otherwise test can pass but JsExport won't be actually respected
data class AltPoint(val x: Int, val y: Int)

// FILE: main.kt
external interface JsResult {
    val res: String
}

@JsModule("lib")
external fun jsBox(): JsResult

fun box(): String {
    val res = jsBox().res
    if (res != "[13::11]") {
        return "Fail1: ${res}"
    }

    return "OK"
}
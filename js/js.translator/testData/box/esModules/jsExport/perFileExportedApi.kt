// DONT_TARGET_EXACT_BACKEND: JS
// SKIP_MINIFICATION
// ES_MODULES
// SPLIT_PER_FILE

// FILE: base.kt

@JsExport
open class Base() {
    var bar = "bar"
    fun foo() = "foo"
}

// FILE: a.kt

@JsExport
class A : Base() {
    val pong = "pong"
    fun ping() = "ping"
}

// FILE: function.kt

@JsExport
fun create() = A()

// FILE: main.kt
@JsModule("./perFileExportedApi.mjs")
external fun jsBox(): String

fun box(): String {
    val res = jsBox()

    if (res != "bar&not bar&foo&ping&pong") {
        return "Fail: ${res}"
    }

    return "OK"
}

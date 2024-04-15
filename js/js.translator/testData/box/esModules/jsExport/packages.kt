// ISSUE: KT-60832
// KT-60832: `O.bar()` is transpiled into `function bar_0()` and cannot be used. `K.bar()` is wrongly used instead of `O.bar()`.
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// SKIP_MINIFICATION
// ES_MODULES

// FILE: apiO.kt
package O
@JsExport fun bar() = "O"

// FILE: apiK.kt
package K
@JsExport fun bar() = "K"


// FILE: main.kt
external interface JsResult {
    val res: String
}

@JsModule("./packages.mjs")
external fun jsBox(): JsResult

fun box(): String {
    return jsBox().res
}
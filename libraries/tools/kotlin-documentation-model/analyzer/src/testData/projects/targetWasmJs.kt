// Wasm/JS-specific declarations: JS interop types, external declarations, and @JsExport.

// MODULE: wasmJsMain
// PLATFORM: WASM_JS
// FILE: pkg/WasmJs.kt
@file:OptIn(ExperimentalJsExport::class)

package pkg

external interface WasmJsOptions : JsAny {
    val title: String
}

external fun jsConsoleLog(message: JsString)

fun toJs(value: String): JsString = value.toJsString()

fun toJsArray(values: List<Int>): JsArray<JsNumber> = values.map { it.toJsNumber() }.toJsArray()

@JsExport
fun exportedSum(a: Int, b: Int): Int = a + b

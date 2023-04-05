// KT-49225
// RUN_PLAIN_BOX_FUNCTION
// IGNORE_BACKEND: JS
// SPLIT_PER_MODULE

// MODULE: lib
// FILE: lib.kt
value class Koo(val koo: String = "OK")

@JsExport
class Bar(val koo: Koo = Koo())

// MODULE: main(lib)
// FILE: main.js
function box() {
    return new kotlin_lib.Bar().koo;
}

// IGNORE_BACKEND: WASM_JS
// ^^^ KT-76462: COMPANION_OBJECT_IN_EXTERNAL_INTERFACE
// ES_MODULES
// FILE: bar.kt
@file:JsModule("./interfacesWithCompanion.mjs")
package bar

external interface Bar {
    companion object {
        fun ok(): String
    }
}

// FILE: test.kt
import bar.Bar

fun box(): String {
    return Bar.ok()
}
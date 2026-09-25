// ISSUE: KT-89363
// LANGUAGE: +CompanionBlocks
// DONT_TARGET_EXACT_BACKEND: WASM_JS
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP

// MODULE: lib
// FILE: lib.kt
package foo

open external class Base() {
    companion object {
        val marker: String
    }
}

// FILE: Base.js
function Base() {}
Base.marker = "X";

// MODULE: main(lib)
// FILE: main.kt
package foo

class DerivedWithObject : Base() {
    companion object {
        val ok = Base.marker + "O"
    }
}

class DerivedWithBlock : Base() {
    companion {
        val ok = Base.marker + "K"
    }
}

class DerivedMixed : Base() {
    companion object {
        val objectOk = Base.marker + "M"
    }

    companion {
        val blockOk = Base.marker + "B"
    }
}

fun box(): String {
    if (DerivedWithObject.ok != "XO") return "FAIL: companion object"
    if (DerivedWithBlock.ok != "XK") return "FAIL: companion block"
    if (DerivedMixed.objectOk != "XM") return "FAIL: mixed object"
    if (DerivedMixed.blockOk != "XB") return "FAIL: mixed block"
    return "OK"
}

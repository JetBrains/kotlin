// ISSUE: KT-89363
// LANGUAGE: +CompanionBlocks
// DONT_TARGET_EXACT_BACKEND: WASM_JS
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP

// FILE: main.kt
open external class Base() {
    companion object
}

open external class MarkedBase() {
    companion object {
        val marker: String
    }
}

class Derived : Base() {
    companion object {
        val ok = "OK"
    }
}

class DerivedWithObject : MarkedBase() {
    companion object {
        val ok = MarkedBase.marker + "O"
    }
}

class DerivedWithBlock : MarkedBase() {
    companion {
        val ok = MarkedBase.marker + "K"
    }
}

class DerivedMixed : MarkedBase() {
    companion object {
        val objectOk = MarkedBase.marker + "M"
    }

    companion {
        val blockOk = MarkedBase.marker + "B"
    }
}

open class MiddleWithoutCompanion : MarkedBase()

class DerivedThroughMiddleWithoutCompanion : MiddleWithoutCompanion() {
    companion object {
        val ok = MarkedBase.marker + "D"
    }
}

open class MiddleWithCompanion : MarkedBase() {
    companion object {
        val middleOk = MarkedBase.marker + "I"
    }
}

class DerivedThroughMiddleWithCompanion : MiddleWithCompanion() {
    companion object {
        val ok = MiddleWithCompanion.middleOk + "E"
    }
}

fun box(): String {
    if (Derived.ok != "OK") return "FAIL: empty companion object"
    if (DerivedWithObject.ok != "XO") return "FAIL: companion object"
    if (DerivedWithBlock.ok != "XK") return "FAIL: companion block"
    if (DerivedMixed.objectOk != "XM") return "FAIL: mixed object"
    if (DerivedMixed.blockOk != "XB") return "FAIL: mixed block"
    if (DerivedThroughMiddleWithoutCompanion.ok != "XD") return "FAIL: middle without companion"
    if (DerivedThroughMiddleWithCompanion.ok != "XIE") return "FAIL: middle with companion"
    return "OK"
}

// FILE: Base.js

function Base() {}
function MarkedBase() {}
MarkedBase.marker = "X";

// RUN_PLAIN_BOX_FUNCTION
// LANGUAGE: +CompanionBlocks +CompanionExtensions

// MODULE: lib
// FILE: lib.kt
@JsExport
class Test

@JsExport
companion fun Test.extFun(value: String = "EXT"): String = value

@JsExport
@JsName("renamedExtFun")
companion fun Test.originalExtFun(value: String = "RENAMED"): String = value

// FILE: main.js
function box() {
    var lib = this.lib;

    if (lib.extFun() !== "EXT") return "FAIL: extFun default"
    if (lib.extFun("CHANGED") !== "CHANGED") return "FAIL: extFun argument"
    if (typeof lib.originalExtFun !== "undefined") return "FAIL: originalExtFun leaked"
    if (lib.renamedExtFun() !== "RENAMED") return "FAIL: renamedExtFun default"
    if (lib.renamedExtFun("CHANGED") !== "CHANGED") return "FAIL: renamedExtFun argument"

    return "OK"
}

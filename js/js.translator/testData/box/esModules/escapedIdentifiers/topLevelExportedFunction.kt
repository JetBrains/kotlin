// ES_MODULES
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// MODULE: lib
// FILE: lib.kt

@JsExport
fun `@do something like-this`(`test value`: Int = 42): Int = `test value`

// FILE: test.mjs
// ENTRY_ES_MODULE
import * as lib from "./topLevelExportedFunction-lib_v5.mjs";

export function box() {
    var value = lib["@do something like-this"]()

    if (value !== 42)
        return "false: expect exproted function '@do something like-this' to return 42 but it equals " + value

    return "OK"
}
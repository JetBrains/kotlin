// ES_MODULES
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// MODULE: lib
// FILE: lib.kt

@JsExport
class `invalid@class-name `() {
    fun foo(): Int = 42
}

// FILE: test.mjs
// ENTRY_ES_MODULE
import { "invalid@class-name " as  InvalidClass } from "./topLevelExportedClass-lib_v5.mjs";

export function box() {
    var instance = new InvalidClass()
    var value = instance.foo()

    if (value !== 42)
        return "false: expect exproted class function to return 42 but it equals " + value

    return "OK"
}
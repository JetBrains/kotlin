// ES_MODULES
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// MODULE: lib
// FILE: lib.kt

@JsExport
class `invalid@class-name `() {
    companion object {
        val `@invalid val@`: Int = 23
        fun `invalid fun`(): Int = 42
    }
}

// FILE: test.mjs
// ENTRY_ES_MODULE
import { "invalid@class-name " as  InvalidClass } from "./topLevelExportedCompanion-lib_v5.mjs";

export function box() {
    if (InvalidClass.Companion["@invalid val@"] !== 23)
        return "false: expect exproted class static variable '@invalid val@' to be 23 but it equals " + InvalidClass.Companion["@invalud val@"]

    var result = InvalidClass.Companion["invalid fun"]()

    if (result !== 42)
        return "false: expect exproted class static function 'invalid fun' to return 23 but it equals " + result

    return "OK"
}